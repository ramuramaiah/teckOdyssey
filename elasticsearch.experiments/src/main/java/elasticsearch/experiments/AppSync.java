package elasticsearch.experiments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

/**
 * Stream data synchronously into Elasticsearch
 */
public class AppSync {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String INDEX_NAME = "shakespeare";
    private static final Logger LOG = LogManager.getLogger(AppSync.class);

    public static void main(String[] args) {
        AppSync app = new AppSync();
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
            ElasticsearchClient client = new ElasticsearchClient(transport);

            try {
                DeleteIndexResponse deleteIndexResponse = client.indices().delete(d -> d
                        .index(indexName)
                        .ignoreUnavailable(true)
                );
                LOG.info("Deleted index acknowledged? {}", deleteIndexResponse.acknowledged());
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

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {
            Rest5ClientTransport transport = new Rest5ClientTransport(
                    restClient,
                    new JacksonJsonpMapper(JSON_MAPPER)
            );
            ElasticsearchClient client = new ElasticsearchClient(transport);

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
                                sendBatchToElasticSearch(linesInBatch, client, indexName);
                                linesInBatch.clear();
                            }
                        } catch (IOException ex) {
                            parseErrors.add(lineText);
                        }
                    }
                    if (!linesInBatch.isEmpty()) {
                        sendBatchToElasticSearch(linesInBatch, client, indexName);
                        linesInBatch.clear();
                    }
                }
            }
        }

        LOG.info("Errors found in {} lines", parseErrors.size());
    }

    /**
     * Load data into Elasticsearch index
     * @param linesToLoad data to load
     * @param client elasticsearch client
     * @param indexName index name
     * @throws IOException
     */
    private void sendBatchToElasticSearch(List<LineFromShakespeare> linesToLoad, ElasticsearchClient client, String indexName)
            throws IOException {
        List<BulkOperation> operations = new ArrayList<>();
        for (LineFromShakespeare l : linesToLoad) {
            BulkOperation operation = BulkOperation.of(b -> b
                    .index(i -> i
                            .index(indexName)
                            .id(l.getId())
                            .document(l)
                    )
            );
            operations.add(operation);
        }

        LOG.info("Sending {} items to ES", operations.size());
        BulkResponse response = client.bulk(b -> b.operations(operations));
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    LOG.error("Bulk request failure for item [{}]: {}", item.id(), item.error().reason());
                }
            }
        }
    }
}
