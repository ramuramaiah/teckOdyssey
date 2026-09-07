package elasticsearch.experiments;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import elasticsearch.experiments.model.CatalogItem;
import elasticsearch.experiments.model.CatalogItemUtil;

/**
 * High-level client CRUD methods
 */
public class HighLevelCrudApp {
	private static final Logger LOG = LogManager.getLogger(HighLevelCrudApp.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static void main(String[] args) {
		try (Rest5Client restClient = Rest5Client.builder(
				new HttpHost("http", "localhost", 9200)
		).build()) {

			Rest5ClientTransport transport = new Rest5ClientTransport(
					restClient,
					new JacksonJsonpMapper(OBJECT_MAPPER)
			);

			ElasticsearchClient syncClient = new ElasticsearchClient(transport);
			ElasticsearchAsyncClient asyncClient = new ElasticsearchAsyncClient(transport);

			// Synchronous operations
			ClientCrudMethods scm = new CrudMethodsSynchronous("catalog_item_high_level", syncClient);
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
				LOG.info("Found item with id {} it is {}", item.getId(), item);
			} else {
				LOG.warn("Could not find item");
			}

			scm.updateCatalogItemDescription(4, "Overwritten description");

			scm.deleteCatalogItem(2);
			Optional<CatalogItem> deletedItem = scm.getCatalogItemById(2);
			if (deletedItem.isPresent()) {
				CatalogItem item = deletedItem.get();
				LOG.info("Found item with id {} it is {}", item.getId(), item);
			} else {
				LOG.info("Item with id 2 was deleted and not found as expected");
			}

			// Asynchronous operations
			CrudMethodsAsynchronous ascm = new CrudMethodsAsynchronous("catalog_item_high_level_async", asyncClient);
			ascm.createCatalogItem(CatalogItemUtil.getCatalogItems());

			CatalogItem itemForU = CatalogItemUtil.getCatalogItems().get(2);

			CompletableFuture<GetResponse<CatalogItem>> future = ascm.getCatalogItemById(itemForU.getId());
			GetResponse<CatalogItem> getResponse = future.join();
			if (getResponse.found() && getResponse.source() != null) {
				CatalogItem catalogItem = getResponse.source();
				LOG.info("item found {} ", catalogItem);
			} else {
				LOG.info("Could not find item with id {}", itemForU.getId());
			}

			CompletableFuture<SearchResponse<CatalogItem>> searchFuture = ascm.findCatalogItem("green");
			SearchResponse<CatalogItem> searchResponse = searchFuture.join();
			List<CatalogItem> catalogItems = ascm.extractSearchResults(searchResponse);
			LOG.info("Found following items containing the word {}. Total items found {} and they are{} :", "green", catalogItems.size(), catalogItems);

			searchFuture = ascm.findCatalogItemByCategory("House");
			searchResponse = searchFuture.join();
			catalogItems = ascm.extractSearchResults(searchResponse);
			LOG.info("Found following items in category {}. Total items found {} and they are{} :", "House", catalogItems.size(), catalogItems);

			itemForU.setPrice(itemForU.getPrice() + .1);
			CompletableFuture<UpdateResponse<CatalogItem>> updateFuture = ascm.updateCatalogItem(itemForU);
			logUpdateStatus(itemForU, updateFuture);

			CompletableFuture<UpdateResponse<Map>> updateDescFuture = ascm.updateCatalogItemDescription(itemForU.getId(), "Updated: " + itemForU.getDescription());
			UpdateResponse<Map> updateDescResponse = updateDescFuture.join();
			if (updateDescResponse.result() == Result.Created) {
				LOG.info("Added catalog item with id {} to ES index {}", itemForU.getId(), updateDescResponse.index());
			} else if (updateDescResponse.result() == Result.Updated) {
				LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", itemForU.getId(), updateDescResponse.index(), updateDescResponse.version());
			}

			CompletableFuture<DeleteResponse> deleteFuture = ascm.deleteCatalogItem(itemForU.getId());
			DeleteResponse deleteResponse = deleteFuture.join();
			if (deleteResponse.result() == Result.NotFound) {
				LOG.info("Could not find catalog item with id {} in ES index {}", deleteResponse.id(), deleteResponse.index());
			} else if (deleteResponse.result() == Result.Deleted) {
				LOG.info("Found and deleted");
			}

		} catch (IOException e) {
			LOG.error("Error while accessing ES", e);
		}
	}

	private static void logUpdateStatus(CatalogItem itemForU, CompletableFuture<UpdateResponse<CatalogItem>> updateFuture) {
		UpdateResponse<CatalogItem> updateResponse = updateFuture.join();
		if (updateResponse.result() == Result.Created) {
			LOG.info("Added catalog item with id {} to ES index {}", updateResponse.id(), updateResponse.index());
		} else if (updateResponse.result() == Result.Updated) {
			LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", updateResponse.id(), updateResponse.index(), updateResponse.version());
		}
	}
}
