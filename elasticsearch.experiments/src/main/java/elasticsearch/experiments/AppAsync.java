package elasticsearch.experiments;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stream data asynchronously into Elasticsearch
 */

public class AppAsync {
    private static final String INDEX_NAME = "shakespeareasync";
	protected static final int NUM_THREADS = 16;
	static Logger LOG = LogManager.getLogger(AppAsync.class);

	public static void main( String[] args ) {
    	AppAsync app = new AppAsync();
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
    		LOG.info("Index deleted {}", deleteIndexResponse.isAcknowledged());
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
    	ObjectMapper objectMapper = new ObjectMapper();
    	List<String> parseErrors = new ArrayList<>();
    	List<LineFromShakespeare> linesInBatch = new ArrayList<>();
    	final int maxLinesInBatch = 1000;
    	final AtomicInteger batchCounter = new AtomicInteger(0);
    	ActionListener<BulkResponse> listener = new ActionListener<BulkResponse>() {
			
			@Override
			public void onResponse(BulkResponse response) {
				batchCounter.decrementAndGet();
			}
			
			@Override
			public void onFailure(Exception e) {
				batchCounter.decrementAndGet();
			}
		};
		RestClientBuilder builder = RestClient.builder(
			    new HttpHost("localhost", 9200))
			    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
			        @Override
			        public HttpAsyncClientBuilder customizeHttpClient(
			                HttpAsyncClientBuilder httpClientBuilder) {
			            return httpClientBuilder.setDefaultIOReactorConfig(
			                IOReactorConfig.custom()
			                    .setIoThreadCount(NUM_THREADS)
			                    .build());
			        }
			    });
    	try(RestHighLevelClient client = new RestHighLevelClient(builder)) {
    		
	    	Files.lines(filePath).forEach( e-> {
	    		try {
					LineFromShakespeare line = objectMapper.readValue(e, LineFromShakespeare.class);
					//enrich...
					linesInBatch.add(line);
					if (linesInBatch.size() >= maxLinesInBatch) {
						sendBatchToElasticSearchAsync(linesInBatch, client, indexName, batchCounter, listener);
					}
				} catch (IOException ex) {
					parseErrors.add(e);
				}
	    	});
	    	if (linesInBatch.size() != 0) {
	    		sendBatchToElasticSearchAsync(linesInBatch, client, indexName, batchCounter, listener);
	    	}
    	}
    	
    	LOG.info("Errors found in {} lines", parseErrors.size());
    	LOG.info("Still processing batches {} ", batchCounter.get());
    	while(batchCounter.get()>0) {
    		try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			} catch (InterruptedException e1) {
				LOG.error("Thead interrupted", e1);
			}
    		LOG.info("Still processing batches {} ", batchCounter.get());
    	}
    }

    /**
     * Load data into Elasticsearch index
     * @param linesToLoad data to load
     * @param client high-level client
     * @param indexName index name
     * @throws IOException
     */
	private void sendBatchToElasticSearchAsync(List<LineFromShakespeare> linesInBatch, RestHighLevelClient client, String indexName, AtomicInteger batchCounter, ActionListener<BulkResponse> listener)
			throws IOException {
		BulkRequest request = new BulkRequest();
		linesInBatch.stream().forEach(l -> {
			try {
				request.add(new IndexRequest(indexName).source(new ObjectMapper().writeValueAsString(l), XContentType.JSON));
			} catch (JsonProcessingException e) {
				LOG.error("Object mapping did not work", e);
			}
		});
		LOG.debug("Sending data to ES - {} - total batches in flight ", batchCounter.get());
		batchCounter.getAndIncrement();
		client.bulkAsync(request, RequestOptions.DEFAULT, listener );
		linesInBatch.clear();
	}
    
}
