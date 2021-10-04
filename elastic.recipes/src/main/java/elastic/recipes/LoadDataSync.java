package elastic.recipes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoadDataSync {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final String INDEX_NAME = "recipes";
	private static int id = 0;
	static Logger LOG = LogManager.getLogger(LoadDataSync.class);

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
	 * Remove all documents from the index identified by index name
	 * 
	 * @param indexName index name
	 * @throws IOException
	 */
	public void cleanUpIndex(String indexName) throws IOException {
		DeleteIndexRequest request = new DeleteIndexRequest(indexName);
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
			// will successeded even if the index is not present
			request.indicesOptions(IndicesOptions.lenientExpandOpen());
			AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
			LOG.info("Deleted index had be acked? {} ", deleteIndexResponse.isAcknowledged());
		}

	}

	public void createIndexWithMapping(String fileName, String indexName) throws IOException, URISyntaxException {
		URI filePathURI = ClassLoader.getSystemResource(fileName).toURI();
		String mappingSource = new String(Files.readAllBytes(Paths.get(filePathURI)));
		
		CreateIndexRequest createIndex = new CreateIndexRequest(indexName);
		createIndex.mapping(mappingSource, XContentType.JSON);
		
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
			AcknowledgedResponse putMappingIndexResponse = client.indices().create(createIndex, RequestOptions.DEFAULT);
			LOG.info("Mapping index had be acked? {} ", putMappingIndexResponse.isAcknowledged());
		}
	}

	/**
	 * Loads data from file identified by file name into an index indentified by
	 * index name f
	 * 
	 * @param fileName  file name
	 * @param indexName index name
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void loadData(String fileName, String indexName) throws IOException, URISyntaxException {
		URI filePathURI = ClassLoader.getSystemResource(fileName).toURI();
		String recipesAsJsonArray = new String(Files.readAllBytes(Paths.get(filePathURI)));
		JsonNode recipesNode = stringToNode(recipesAsJsonArray);
		List<ObjectNode> recipesNodeList = arrayNodeToList((ArrayNode) recipesNode);
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")))) {

			sendBatchToElasticSearch(recipesNodeList, client, indexName);
		}
	}

	/**
	 * Load data into Elasticsearch index
	 * 
	 * @param recipesToLoad data to load
	 * @param client        high-level client
	 * @param indexName     index name
	 * @throws IOException
	 */
	private void sendBatchToElasticSearch(List<ObjectNode> recipesToLoad, RestHighLevelClient client, String indexName)
			throws IOException {
		BulkRequest request = new BulkRequest();
		recipesToLoad.stream().forEach(r -> {
			try {
				request.add(new IndexRequest(indexName).id(Integer.toString(id++))
						.source(JSON_MAPPER.writeValueAsString(r), XContentType.JSON));
			} catch (JsonProcessingException e) {
				LOG.error("Problem mapping object {}", r, e);
			}
		});
		LOG.info("Sending data to ES");
		BulkResponse bulk = client.bulk(request, RequestOptions.DEFAULT);
		boolean hasFailures = bulk.hasFailures();
		LOG.info("Bulk request has failures? : " + hasFailures);
		if(hasFailures) {
			Iterator<BulkItemResponse> iterator = bulk.iterator();
			while(iterator.hasNext()) {
				BulkItemResponse resp = iterator.next();
				LOG.error("Bulk request status failure: " + resp.getFailureMessage());
			}
		}
		
	}

	private JsonNode stringToNode(String str) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node;
		try {
			node = mapper.readTree(str.getBytes("UTF-8"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return node;
	}

	private List<ObjectNode> arrayNodeToList(ArrayNode node) {
		List<ObjectNode> list = new ArrayList<ObjectNode>();

		for (int i = 0; i < node.size(); i++) {
			JsonNode jsonNode = node.get(i);
			if (jsonNode instanceof ObjectNode) {
				list.add((ObjectNode) jsonNode);
			}
		}
		return list;
	}
}
