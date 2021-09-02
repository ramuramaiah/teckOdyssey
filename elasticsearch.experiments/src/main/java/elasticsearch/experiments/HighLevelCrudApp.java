package elasticsearch.experiments;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import elasticsearch.experiments.model.CatalogItem;
import elasticsearch.experiments.model.CatalogItemUtil;


/**
 * High-level client CRUD methods
 */
public class HighLevelCrudApp {
	static Logger LOG = LogManager.getLogger(HighLevelCrudApp.class);

	public static void main( String[] args ) {
		try(RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
			ClientCrudMethods scm = new CrudMethodsSynchronous("catalog_item_high_level",  client);
			scm.createCatalogItem(CatalogItemUtil.getCatalogItems());
			
			
			List<CatalogItem> items = scm.findCatalogItem("flashlight");
			LOG.info("Found {} items: {}", items.size(), items);
			
			items = scm.findCatalogItemByCategory("House");
			LOG.info("Found {} items by category: {}", items.size(), items);
			
			items = scm.findCatalogItemByCategoryAndSort("Office");
			LOG.info("Found {} items by category and sort: {}", items.size(), items);
			
			CatalogItem itemForUpdate = CatalogItemUtil.getCatalogItems().get(3);
			itemForUpdate.setDescription("Updated : " + itemForUpdate.getDescription());
			scm.updateCatalogItem(itemForUpdate);
			
			Optional<CatalogItem> foundItem = scm.getCatalogItemById(itemForUpdate.getId());
			if (foundItem.isPresent()) {
				CatalogItem item = foundItem.get();
				LOG.info("Found item with id {} it is {}", item.getId(), item );
			} else {
				LOG.warn("Could not find in item");
			}
			
			scm.updateCatalogItemDescription(4, "Overwritten description");
			
			scm.deleteCatalogItem(2);
			Optional<CatalogItem> deletedItem = scm.getCatalogItemById(2);
			if (foundItem.isPresent()) {
				CatalogItem item = foundItem.get();
				LOG.info("Found item with id {} it is {}", item.getId(), item );
			} else {
				LOG.warn("Could not find in item");
			}
			
			
			CrudMethodsAsynchronous ascm = new CrudMethodsAsynchronous("catalog_item_high_level_async",  client);
			ascm.createCatalogItem(CatalogItemUtil.getCatalogItems());
			
			CatalogItem itemForU = CatalogItemUtil.getCatalogItems().get(2);
			
			
			PlainActionFuture<GetResponse> future = ascm.getCatalogItemById(itemForU.getId());
			GetResponse getResponse = future.actionGet();
			if (getResponse.isExists()) {
				    String json = getResponse.getSourceAsString();
				    CatalogItem catalogItem = ascm.getObjectMapper().readValue(json, CatalogItem.class);
					LOG.info("item found {} ", catalogItem);
			} else {
				LOG.info("Could not find item with id {}", itemForU.getId());
			}
			
			PlainActionFuture<SearchResponse> searchFuture = ascm.findCatalogItem("green");
			List<CatalogItem> catalogItems = getSearchResults(ascm, searchFuture);
			
			LOG.info("Found following items containing the word {}. Total items found {} and they are{} :", "green", catalogItems.size(), catalogItems);
			
			searchFuture = ascm.findCatalogItemByCategory("House");
			catalogItems = getSearchResults(ascm, searchFuture);
			LOG.info("Found following items containing the word in category {}. Total items found {} and they are{} :", "green", catalogItems.size(), catalogItems);
			
			itemForU.setPrice(itemForU.getPrice() + .1);
			PlainActionFuture<UpdateResponse> updateFuture = ascm.updateCatalogItem(itemForU);
			logUpdateStatus(itemForU, updateFuture);

			updateFuture = ascm.updateCatalogItemDescription(itemForU.getId(), "Updated: " + itemForU.getDescription());
			logUpdateStatus(itemForU, updateFuture);
			
			PlainActionFuture<DeleteResponse> deleteFuture = ascm.deleteCatalogItem(itemForU.getId());
			DeleteResponse deleteResponse = deleteFuture.actionGet();
			if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
				LOG.info("Could not find catalog item with id {} to ES index {}", deleteResponse.getId(), deleteResponse.getIndex());
			} else if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
				LOG.info("Found and deleted");
			}
			
			
		} catch(IOException e) {
			LOG.error("Error while accessing ES", e);
		}
    }

	private static void logUpdateStatus(CatalogItem itemForU, PlainActionFuture<UpdateResponse> updateFuture) {
		UpdateResponse updateResponse = updateFuture.actionGet();
		if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
			LOG.info("Added catalog item with id {} to ES index {}", updateResponse.getId(), updateResponse.getIndex());  
		} else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
			LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {} ", updateResponse.getId(), updateResponse.getIndex(), updateResponse.getVersion());
		}
	}

	private static List<CatalogItem> getSearchResults(CrudMethodsAsynchronous ascm,
			PlainActionFuture<SearchResponse> searchFuture) {
		SearchResponse searchResponse = searchFuture.actionGet();
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
		List<CatalogItem> catalogItems = Arrays.stream(searchHits)
											.map(e -> ascm.mapJsonToCatalogItem(e.getSourceAsString()))
											.filter(Objects::nonNull)	
											.collect(Collectors.toList());
		return catalogItems;
	}
     
	
}
