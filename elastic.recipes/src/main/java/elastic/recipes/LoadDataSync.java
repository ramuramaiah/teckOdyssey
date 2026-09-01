package elastic.recipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;


public class LoadDataSync {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String INDEX_NAME = "recipes";

    private static int id = 0;

    private static final Logger LOG =
            LogManager.getLogger(LoadDataSync.class);


    public static void main(String[] args) {

        LoadDataSync app = new LoadDataSync();

        try {
            app.cleanUpIndex(INDEX_NAME);
            app.createIndexWithMapping("mappings.json", INDEX_NAME);
            app.loadData("recipes.json", INDEX_NAME);

        } catch (Exception e) {
            LOG.error("Could not load data", e);
        }

        LOG.info("Done");
    }


    /**
     * Creates an Elasticsearch Java API client.
     * <p>
     * Elasticsearch server:
     * http://localhost:9200
     */
    private ElasticsearchClient createClient() {

        Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build();

        Rest5ClientTransport transport =
                new Rest5ClientTransport(
                        restClient,
                        new JacksonJsonpMapper(JSON_MAPPER)
                );

        return new ElasticsearchClient(transport);
    }


    /**
     * Remove the index identified by indexName.
     * <p>
     * Elasticsearch 9.x Java API Client.
     */
    public void cleanUpIndex(String indexName) throws IOException {

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {

            Rest5ClientTransport transport =
                    new Rest5ClientTransport(
                            restClient,
                            new JacksonJsonpMapper(JSON_MAPPER)
                    );

            ElasticsearchClient client =
                    new ElasticsearchClient(transport);

            try {

                DeleteIndexResponse response =
                        client.indices().delete(d -> d
                                .index(indexName)
                        );

                LOG.info(
                        "Index [{}] deleted? {}",
                        indexName,
                        response.acknowledged()
                );

            } catch (Exception e) {
                LOG.info(e.getMessage());
            }
        }
    }


    /**
     * Creates an Elasticsearch index using the mapping contained
     * in mappings.json.
     */
    public void createIndexWithMapping(
            String fileName,
            String indexName)
            throws IOException {

        String mappingSource;

        try (InputStream is =
                     LoadDataSync.class.getClassLoader()
                             .getResourceAsStream(fileName)) {

            if (is == null) {
                throw new IOException(
                        "Resource not found on classpath: " + fileName
                );
            }

            mappingSource = new String(is.readAllBytes());
        }

        /*
         * The mappings.json file is expected to contain:
         *
         * {
         *   "properties": {
         *       ...
         *   }
         * }
         *
         * We deserialize it into JsonNode and pass the individual
         * properties to the new Java API client.
         */
        JsonNode mappingNode =
                JSON_MAPPER.readTree(mappingSource);

        String mappingJson =
                JSON_MAPPER.writeValueAsString(mappingNode);

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {

            Rest5ClientTransport transport =
                    new Rest5ClientTransport(
                            restClient,
                            new JacksonJsonpMapper(JSON_MAPPER)
                    );

            ElasticsearchClient client =
                    new ElasticsearchClient(transport);

            CreateIndexResponse response =
                    client.indices().create(c -> c
                            .index(indexName)
                            .mappings(m -> m
                                    .withJson(new StringReader(mappingJson))
                            )
                    );

            LOG.info(
                    "Index [{}] created? {}",
                    indexName,
                    response.acknowledged()
            );
        }
    }


    /**
     * Loads data from JSON file into Elasticsearch.
     */
    public void loadData(
            String fileName,
            String indexName)
            throws IOException {

        String recipesAsJsonArray;

        try (InputStream is =
                     LoadDataSync.class.getClassLoader()
                             .getResourceAsStream(fileName)) {

            if (is == null) {
                throw new IOException(
                        "Resource not found on classpath: " + fileName
                );
            }

            recipesAsJsonArray = new String(is.readAllBytes());
        }

        JsonNode recipesNode =
                stringToNode(recipesAsJsonArray);

        if (!recipesNode.isArray()) {
            throw new IllegalArgumentException(
                    "Expected recipes.json to contain a JSON array"
            );
        }

        List<ObjectNode> recipesNodeList =
                arrayNodeToList((ArrayNode) recipesNode);

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {

            Rest5ClientTransport transport =
                    new Rest5ClientTransport(
                            restClient,
                            new JacksonJsonpMapper(JSON_MAPPER)
                    );

            ElasticsearchClient client =
                    new ElasticsearchClient(transport);

            sendBatchToElasticSearch(
                    recipesNodeList,
                    client,
                    indexName
            );
        }
    }


    /**
     * Loads documents using Elasticsearch Bulk API.
     */
    private void sendBatchToElasticSearch(
            List<ObjectNode> recipesToLoad,
            ElasticsearchClient client,
            String indexName)
            throws IOException {

        List<BulkOperation> operations =
                new ArrayList<>();

        for (ObjectNode recipe : recipesToLoad) {

            String documentId =
                    Integer.toString(id++);

            BulkOperation operation =
                    BulkOperation.of(b -> b
                            .index(i -> i
                                    .index(indexName)
                                    .id(documentId)
                                    .document(recipe)
                            )
                    );

            operations.add(operation);
        }

        LOG.info(
                "Sending {} documents to Elasticsearch",
                operations.size()
        );

        BulkResponse bulkResponse =
                client.bulk(b -> b
                        .operations(operations)
                );

        LOG.info(
                "Bulk request has failures? : {}",
                bulkResponse.errors()
        );

        if (bulkResponse.errors()) {

            for (BulkResponseItem item :
                    bulkResponse.items()) {

                if (item.error() != null) {

                    LOG.error(
                            "Bulk request failure for document [{}]: {}",
                            item.id(),
                            item.error().reason()
                    );
                }
            }
        }
    }


    /**
     * Converts JSON string to JsonNode.
     */
    private JsonNode stringToNode(String str) {

        try {
            return JSON_MAPPER.readTree(str);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Converts JSON array into List<ObjectNode>.
     */
    private List<ObjectNode> arrayNodeToList(
            ArrayNode node) {

        List<ObjectNode> list =
                new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {

            JsonNode jsonNode = node.get(i);

            if (jsonNode instanceof ObjectNode) {
                list.add((ObjectNode) jsonNode);
            }
        }

        return list;
    }
}
