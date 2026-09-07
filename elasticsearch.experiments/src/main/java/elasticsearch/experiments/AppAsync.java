package elasticsearch.experiments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

/**
 * Stream data asynchronously into Elasticsearch
 */
public class AppAsync {
    private static final String INDEX_NAME = "shakespeareasync";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Logger LOG = LogManager.getLogger(AppAsync.class);

    public static void main(String[] args) {
        AppAsync app = new AppAsync();
        try {
            app.cleanUpIndex(INDEX_NAME);
            app.loadData("shakespeareLines.json", INDEX_NAME);
        } catch (Exception e) {
            LOG.error("Could not load data", e);
        }
        LOG.info("Done");
    }

    /**
     * Remove all documents from the index identified by index name
     * @param indexName index name
     * @throws IOException 
     */
    public void cleanUpIndex(String indexName) throws IOException {
        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {
            Rest5ClientTransport transport = new Rest5ClientTransport(
                    restClient,
                    new JacksonJsonpMapper(JSON_MAPPER)
            );
            ElasticsearchAsyncClient client = new ElasticsearchAsyncClient(transport);

            try {
                DeleteIndexResponse deleteIndexResponse = client.indices().delete(d -> d
                        .index(indexName)
                        .ignoreUnavailable(true)
                ).join();
                LOG.info("Index deleted {}", deleteIndexResponse.acknowledged());
            } catch (Exception e) {
                LOG.info("Could not delete index (may not exist): {}", e.getMessage());
            }
        }
    }

    /**
     * Loads data from file identified by file name into an index identified by index name
     * @param fileName file name
     * @param indexName index name
     * @throws IOException
     */
    public void loadData(String fileName, String indexName) throws IOException {
        List<String> parseErrors = new ArrayList<>();
        List<LineFromShakespeare> linesInBatch = new ArrayList<>();
        final int maxLinesInBatch = 1000;
        final AtomicInteger batchCounter = new AtomicInteger(0);

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {
            Rest5ClientTransport transport = new Rest5ClientTransport(
                    restClient,
                    new JacksonJsonpMapper(JSON_MAPPER)
            );
            ElasticsearchAsyncClient client = new ElasticsearchAsyncClient(transport);

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (is == null) {
                    throw new IOException("Resource not found on classpath: " + fileName);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String lineText;
                    while ((lineText = reader.readLine()) != null) {
                        try {
                            LineFromShakespeare line = JSON_MAPPER.readValue(lineText, LineFromShakespeare.class);
                            linesInBatch.add(line);
                            if (linesInBatch.size() >= maxLinesInBatch) {
                                sendBatchToElasticSearchAsync(new ArrayList<>(linesInBatch), client, indexName, batchCounter);
                                linesInBatch.clear();
                            }
                        } catch (IOException ex) {
                            parseErrors.add(lineText);
                        }
                    }
                    if (!linesInBatch.isEmpty()) {
                        sendBatchToElasticSearchAsync(new ArrayList<>(linesInBatch), client, indexName, batchCounter);
                        linesInBatch.clear();
                    }
                }
            }

            LOG.info("Errors found in {} lines", parseErrors.size());
            LOG.info("Still processing batches {}", batchCounter.get());
            while (batchCounter.get() > 0) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e1) {
                    LOG.error("Thread interrupted", e1);
                    Thread.currentThread().interrupt();
                    break;
                }
                LOG.info("Still processing batches {}", batchCounter.get());
            }
        }
    }

    /**
     * Load data into Elasticsearch index asynchronously
     * @param linesInBatch data to load
     * @param client async client
     * @param indexName index name
     * @param batchCounter counter tracking active in-flight batches
     */
    private void sendBatchToElasticSearchAsync(List<LineFromShakespeare> linesInBatch, ElasticsearchAsyncClient client, String indexName, AtomicInteger batchCounter) {
        List<BulkOperation> operations = new ArrayList<>();
        for (LineFromShakespeare l : linesInBatch) {
            BulkOperation operation = BulkOperation.of(b -> b
                    .index(i -> i
                            .index(indexName)
                            .id(l.getId())
                            .document(l)
                    )
            );
            operations.add(operation);
        }

        LOG.debug("Sending data to ES - {} - total batches in flight", batchCounter.get());
        batchCounter.incrementAndGet();

        client.bulk(b -> b.operations(operations)).whenComplete((bulkResponse, throwable) -> {
            batchCounter.decrementAndGet();
            if (throwable != null) {
                LOG.error("Bulk async request failed: ", throwable);
            } else if (bulkResponse.errors()) {
                for (BulkResponseItem item : bulkResponse.items()) {
                    if (item.error() != null) {
                        LOG.error("Bulk request failure for item [{}]: {}", item.id(), item.error().reason());
                    }
                }
            }
        });
    }
}
