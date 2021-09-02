package elasticsearch.experiments;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import elasticsearch.experiments.model.CatalogItem;

/**
 * Example of high-level synchronous client CRUD methods
 */
public class CrudMethodsSynchronous implements ClientCrudMethods {
	static Logger LOG = LogManager.getLogger(CrudMethodsSynchronous.class);
	private final String index;
	private final RestHighLevelClient restClient;
	private final ObjectMapper objectMapper;
	
	public CrudMethodsSynchronous(String index, RestHighLevelClient restClient) {
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
	 * @inheritDoc
	 */
	@Override
	public void createCatalogItem(List<CatalogItem> items) {
		items.stream().forEach(e-> {
			IndexRequest request = new IndexRequest(getIndex());
			try {
				request.id(""+e.getId());
				request.source(getObjectMapper().writeValueAsString(e), XContentType.JSON);
				request.timeout(TimeValue.timeValueSeconds(10));
				IndexResponse indexResponse = getRestClient().index(request, RequestOptions.DEFAULT);
				if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
				  LOG.info("Added catalog item with id {} to ES index {}", e.getId(), indexResponse.getIndex());  
				} else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
					LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {} ", e.getId(), indexResponse.getIndex(), indexResponse.getVersion()); 
				} 
	
			} catch (IOException ex) {
				LOG.warn("Could not post {} to ES", e, ex);
			}
		});
	}
	
	
	/**
	 * @inheritDoc
	 */
	@Override
	public Optional<CatalogItem> getCatalogItemById(Integer idToFind) {
		GetRequest request = new GetRequest(getIndex(),""+idToFind); 

		try {
			GetResponse response = getRestClient().get(request, RequestOptions.DEFAULT);
			if (response.isExists()) {
			    String json = response.getSourceAsString();
			    CatalogItem catalogItem = objectMapper.readValue(json, CatalogItem.class);
				return Optional.of(catalogItem);
			} else {
				return Optional.empty();
			}
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", idToFind, ex);
		}
		return Optional.empty();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItem(String text) {
		try {
			SearchRequest searchRequest = new SearchRequest(getIndex()); 
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			SimpleQueryStringBuilder matchQueryBuilder = QueryBuilders.simpleQueryStringQuery(text);
			searchSourceBuilder.query(matchQueryBuilder); 
			searchRequest.source(searchSourceBuilder);
			System.out.println("findCatalogItem query: " + matchQueryBuilder.toString());
			
			SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			List<CatalogItem> catalogItems = Arrays.stream(searchHits)
												.map(e -> mapJsonToCatalogItem(e.getSourceAsString()))
												.filter(Objects::nonNull)	
												.collect(Collectors.toList());
			
			return catalogItems;
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", text, ex);
		}
		return Collections.emptyList();
		
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
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItemByCategory(String token) {
		try {
			SearchRequest searchRequest = new SearchRequest(getIndex()); 
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("category.category_name",token);
			searchSourceBuilder.query(matchQueryBuilder); 
			searchRequest.source(searchSourceBuilder);
			
			System.out.println("findCatalogItemByCategory query: " + matchQueryBuilder.toString());
			
			SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			List<CatalogItem> catalogItems = Arrays.stream(searchHits)
												.map(e -> mapJsonToCatalogItem(e.getSourceAsString()))
												.filter(Objects::nonNull)	
												.collect(Collectors.toList());
			
			return catalogItems;
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", token, ex);
		}
		return Collections.emptyList();
			
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItemByCategoryAndSort(String token) {
		try {
			SearchRequest searchRequest = new SearchRequest(getIndex()); 
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("category.category_name",token);
			searchSourceBuilder.query(matchQueryBuilder);
			searchSourceBuilder.sort(new FieldSortBuilder("price").order(SortOrder.ASC));
			searchRequest.source(searchSourceBuilder);
			
			System.out.println("findCatalogItemByCategoryAndSort query: " + searchSourceBuilder.toString());
			
			SearchResponse response = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			List<CatalogItem> catalogItems = Arrays.stream(searchHits)
												.map(e -> mapJsonToCatalogItem(e.getSourceAsString()))
												.filter(Objects::nonNull)	
												.collect(Collectors.toList());
			
			return catalogItems;
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", token, ex);
		}
		return Collections.emptyList();
	}	
	
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void updateCatalogItem(CatalogItem item) {
		UpdateRequest request = new UpdateRequest(getIndex(), ""+item.getId());
		try {

			request.doc(getObjectMapper().writeValueAsString(item), XContentType.JSON);
			UpdateResponse response = getRestClient().update(request, RequestOptions.DEFAULT);
			if (response.getResult() == DocWriteResponse.Result.CREATED) {
				LOG.info("Added catalog item with id {} to ES index {}", item.getId(), response.getIndex());  
			} else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
				LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {} ", item.getId(), response.getIndex(), response.getVersion());
			} 
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", item, ex);
		}
		
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void updateCatalogItemDescription(Integer id, String description) {
		UpdateRequest request = new UpdateRequest(getIndex(), ""+id);
		try {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("description", description);
			request.doc(jsonMap);
			UpdateResponse response = getRestClient().update(request, RequestOptions.DEFAULT);
			if (response.getResult() == DocWriteResponse.Result.CREATED) {
				LOG.info("Added catalog item with id {} to ES index {}", id, response.getIndex());  
			} else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
				LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {} ", id, response.getIndex(), response.getVersion());
			} 
		} catch (IOException ex) {
			LOG.warn("Could not update catalog item bu id {} to ES", id, ex);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteCatalogItem(Integer id) {
		DeleteRequest request = new DeleteRequest(getIndex(), ""+id);
		try {
			DeleteResponse response = getRestClient().delete(request, RequestOptions.DEFAULT);
			if (response.getResult() == DocWriteResponse.Result.NOT_FOUND) {
				LOG.info("Could not find catalog item with id {} to ES index {}", id, response.getIndex());
			} else if (response.getResult() == DocWriteResponse.Result.DELETED) {
				LOG.info("Found and deleted");
			}
		} catch (IOException ex) {
			LOG.warn("Could not delete from catalog item bu id {} to ES", id, ex);
		}	
		
	}
}
