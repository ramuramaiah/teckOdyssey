package elasticsearch.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import elasticsearch.experiments.model.CatalogItem;

/**
 * Example of high-level asynchronous client CRUD methods
 *
 */
public class CrudMethodsAsynchronous {
	static Logger LOG = LogManager.getLogger(CrudMethodsAsynchronous.class);
	private final String index;
	private final RestHighLevelClient restClient;
	private final ObjectMapper objectMapper;
	
	
	public CrudMethodsAsynchronous(String index, RestHighLevelClient restClient) {
		this.index = index;
		this.restClient = restClient;
		this.objectMapper = new ObjectMapper();
	}
	
	
	protected String getIndex() {
		return index;
	}


	protected RestHighLevelClient getRestClient() {
		return restClient;
	}


	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Adds an items to Elasticsearch index
	 * @param items items to be added
	 */
	public void createCatalogItem(List<CatalogItem> items) {
		CountDownLatch countDownLatch = new CountDownLatch(items.size());
		ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
			@Override
			public void onResponse(IndexResponse response) {
				countDownLatch.countDown();
			}
			@Override
			public void onFailure(Exception exception) {
				countDownLatch.countDown();
				LOG.error("Could not process ES request. Error : ", exception);
			}
			
		};
		
		items.stream().forEach(e-> {
			IndexRequest request = new IndexRequest(getIndex());
			try {
				request.id(""+e.getId());
				request.source(getObjectMapper().writeValueAsString(e), XContentType.JSON);
				request.timeout(TimeValue.timeValueSeconds(10));
				
				getRestClient().indexAsync(request, RequestOptions.DEFAULT, listener);
				
	
			} catch (IOException ex) {
				LOG.warn("Could not post {} to ES", e, ex);
			}
		});
		try {
			countDownLatch.await(); //wait for all the threads to finish
			LOG.info("Done inserting all the records to the index {}. Total # of records inserted is {}", getIndex(), items);
		} catch (InterruptedException e1) {
			LOG.warn("Got interrupted waiting for all the clients",e1);
		}
		
	}
	
	
	/**
	 * Returns a future containing an item in the index based on specified id
	 * @param idToFind id
	 * @return  a future containing an item in the index based on specified id
	 */
	public PlainActionFuture<GetResponse> getCatalogItemById(Integer idToFind) {
		GetRequest request = new GetRequest(getIndex(),""+idToFind); 

		PlainActionFuture<GetResponse> future = new PlainActionFuture<>();
		getRestClient().getAsync(request, RequestOptions.DEFAULT, future);
		return future;
			
	}

	
	/**
	 * Performs full text search on the index and returns items that matched
	 * @param text search text
	 * @return  a future containing items that matched the search
	 */
	public PlainActionFuture<SearchResponse> findCatalogItem(String text) {
		SearchRequest searchRequest = new SearchRequest(getIndex()); 
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		SimpleQueryStringBuilder matchQueryBuilder = QueryBuilders.simpleQueryStringQuery(text);
		searchSourceBuilder.query(matchQueryBuilder); 
		searchRequest.source(searchSourceBuilder);
		
		PlainActionFuture<SearchResponse> future = new PlainActionFuture<>();
		getRestClient().searchAsync(searchRequest, RequestOptions.DEFAULT, future);
		return future;
			
	}

	/**
	 * Returns catalog item based on JSON passed
	 * @param json JSON passed 
	 * @return catalog item based on JSON passed
	 */
	protected CatalogItem mapJsonToCatalogItem(String json) {
		try {
			return getObjectMapper().readValue(json, CatalogItem.class);
		} catch (IOException e) {
			LOG.warn("Could not convert {} to CatalogItem", json);
		}
		return null;
	}
	
	/**
	 * Returns  a future containing items that match category token
	 * @param token category token
	 * @return  a future containing items that match category token
	 */
	public PlainActionFuture<SearchResponse> findCatalogItemByCategory(String token) {
			SearchRequest searchRequest = new SearchRequest(getIndex()); 
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("category.category_name",token);
			searchSourceBuilder.query(matchQueryBuilder); 
			searchRequest.source(searchSourceBuilder);
			PlainActionFuture<SearchResponse> future = new PlainActionFuture<>();
			getRestClient().searchAsync(searchRequest, RequestOptions.DEFAULT, future);
			return future;
	}
	
	
	/**
	 * Updates document in Elasticsearch
	 * @param item document to update
	 * @return  a future containing update status
	 * @throws JsonProcessingException
	 */
	public PlainActionFuture<UpdateResponse> updateCatalogItem(CatalogItem item) throws JsonProcessingException {
		UpdateRequest request = new UpdateRequest(getIndex(), ""+item.getId());

			request.doc(getObjectMapper().writeValueAsString(item), XContentType.JSON);
			PlainActionFuture<UpdateResponse> future = new PlainActionFuture<>();
			getRestClient().updateAsync(request, RequestOptions.DEFAULT, future);
			return future;
	}
	
	/**
	 * Updates just the description field of a document base on id
	 * @param id id of the document
	 * @param description new description
	 * @return  a future containing update status
	 */
	public PlainActionFuture<UpdateResponse> updateCatalogItemDescription(Integer id, String description) {
		UpdateRequest request = new UpdateRequest(getIndex(), ""+id);
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("description", description);
			request.doc(jsonMap);
			PlainActionFuture<UpdateResponse> future = new PlainActionFuture<>();
			getRestClient().updateAsync(request, RequestOptions.DEFAULT, future);
			return future;
	}
	
	/**
	 * Deletes a document from an index
	 * @param id id of a document to be deleted
	 * @return  a future containing delete status
	 */
	public PlainActionFuture<DeleteResponse> deleteCatalogItem(Integer id) {
		DeleteRequest request = new DeleteRequest(getIndex(), ""+id);
		PlainActionFuture<DeleteResponse> future = new PlainActionFuture<>();
		getRestClient().deleteAsync(request, RequestOptions.DEFAULT, future);
		return future;
	}
	
}
