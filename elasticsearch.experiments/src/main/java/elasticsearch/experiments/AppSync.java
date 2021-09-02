package elasticsearch.experiments;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stream data synchronously into Elasticsearch
 *
 */
public class AppSync {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final String INDEX_NAME = "shakespeare";
    static Logger LOG = LogManager.getLogger(AppSync.class);
    
	public static void main( String[] args ) {
    	AppSync app = new AppSync();
    	try {
    		app.cleanUpIndex(INDEX_NAME);
			app.loadData("shakespeareLines.json", INDEX_NAME);
		} catch (Exception e) {
			LOG.error("Could not load data",e);
		}
        LOG.info("Done");
    }
    
	/**
	 * Remove all documents from the index identified by index name
	 * @param indexName index name
	 * @throws IOException 
	 */
    public void cleanUpIndex(String indexName) throws IOException {
    	DeleteIndexRequest request = new DeleteIndexRequest(indexName);
    	try(RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")))) {
    		//will successeded even if the index is not present
    		request.indicesOptions(IndicesOptions.lenientExpandOpen());
    		AcknowledgedResponse deleteIndexResponse = 
    				client.indices().delete(request, RequestOptions.DEFAULT);
    		 LOG.info("Deleted index had be acked? {} ",deleteIndexResponse.isAcknowledged());
    	}
    	
    }
    
    /**
     * Loads data from file identified by file name into an index indentified by index name f
     * @param fileName file name
     * @param indexName index name
     * @throws IOException
     * @throws URISyntaxException
     */
    public void loadData(String fileName, String indexName) throws IOException, URISyntaxException {
    	Path filePath = Paths.get(ClassLoader.getSystemResource(fileName).toURI());
    	ObjectMapper objectMapper = JSON_MAPPER;
    	List<String> parseErrors = new ArrayList<>();
    	List<LineFromShakespeare> linesInBatch = new ArrayList<>();
    	final int maxLinesInBatch = 1000;
    	try(RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")))) {
    		
	    	Files.lines(filePath).forEach( e-> {
	    		try {
					LineFromShakespeare line = objectMapper.readValue(e, LineFromShakespeare.class);
					//enrich...
					linesInBatch.add(line);
					if (linesInBatch.size() >= maxLinesInBatch) {
						sendBatchToElasticSearch(linesInBatch, client, indexName);
						linesInBatch.clear();
					}
				} catch (IOException ex) {
					parseErrors.add(e);
					linesInBatch.clear();
				}
	    	});
	    	if (linesInBatch.size() != 0) {
	    		sendBatchToElasticSearch(linesInBatch, client, indexName);
	    		linesInBatch.clear();
	    	}
    	}
    	
    	LOG.info("Errors found in {} batches", parseErrors.size());
    }

    /**
     * Load data into Elasticsearch index
     * @param linesToLoad data to load
     * @param client high-level client
     * @param indexName index name
     * @throws IOException
     */
	private void sendBatchToElasticSearch(List<LineFromShakespeare> linesToLoad, RestHighLevelClient client, String indexName)
			throws IOException {
		BulkRequest request = new BulkRequest();
		linesToLoad.stream().forEach(l -> {
			try {
				request.add(new IndexRequest(indexName)
									.id(l.getId())
									.source(JSON_MAPPER.writeValueAsString(l), XContentType.JSON));
			} catch (JsonProcessingException e) {
				LOG.error("Problem mapping object {}", l, e);
			}
		});
		LOG.info("Sending data to ES");
		client.bulk(request, RequestOptions.DEFAULT);
	}
    
}
