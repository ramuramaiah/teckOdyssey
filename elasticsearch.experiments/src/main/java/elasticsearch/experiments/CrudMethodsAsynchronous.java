package elasticsearch.experiments;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import elasticsearch.experiments.model.CatalogItem;

/**
 * Example of high-level asynchronous client CRUD methods
 */
public class CrudMethodsAsynchronous {
	private static final Logger LOG = LogManager.getLogger(CrudMethodsAsynchronous.class);
	private final String index;
	private final ElasticsearchAsyncClient asyncClient;
	private final ObjectMapper objectMapper;
	
	public CrudMethodsAsynchronous(String index, ElasticsearchAsyncClient asyncClient) {
		this.index = index;
		this.asyncClient = asyncClient;
		this.objectMapper = new ObjectMapper();
	}
	
	protected String getIndex() {
		return index;
	}

	protected ElasticsearchAsyncClient getAsyncClient() {
		return asyncClient;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Adds items to Elasticsearch index asynchronously
	 * @param items items to be added
	 */
	public void createCatalogItem(List<CatalogItem> items) {
		CountDownLatch countDownLatch = new CountDownLatch(items.size());

		items.forEach(e -> {
			getAsyncClient().index(i -> i
					.index(getIndex())
					.id(String.valueOf(e.getId()))
					.document(e)
			).whenComplete((response, exception) -> {
				countDownLatch.countDown();
				if (exception != null) {
					LOG.error("Could not process ES request for item with id {}. Error : ", e.getId(), exception);
				} else {
					if (response.result() == Result.Created) {
						LOG.info("Added catalog item with id {} to ES index {}", e.getId(), response.index());
					} else if (response.result() == Result.Updated) {
						LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", e.getId(), response.index(), response.version());
					}
				}
			});
		});

		try {
			countDownLatch.await(); // wait for all the requests to finish
			LOG.info("Done inserting all the records to the index {}. Total # of records inserted is {}", getIndex(), items.size());
		} catch (InterruptedException e1) {
			LOG.warn("Got interrupted waiting for all the clients", e1);
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Returns a CompletableFuture containing an item in the index based on specified id
	 * @param idToFind id
	 * @return a CompletableFuture containing the GetResponse
	 */
	public CompletableFuture<GetResponse<CatalogItem>> getCatalogItemById(Integer idToFind) {
		return getAsyncClient().get(g -> g
				.index(getIndex())
				.id(String.valueOf(idToFind)),
				CatalogItem.class
		);
	}

	/**
	 * Performs full text search on the index and returns items that matched
	 * @param text search text
	 * @return a CompletableFuture containing the SearchResponse
	 */
	public CompletableFuture<SearchResponse<CatalogItem>> findCatalogItem(String text) {
		LOG.info("findCatalogItem async query: simple_query_string({})", text);
		return getAsyncClient().search(s -> s
				.index(getIndex())
				.query(q -> q
						.simpleQueryString(sqs -> sqs
								.query(text)
						)
				),
				CatalogItem.class
		);
	}

	/**
	 * Returns a CompletableFuture containing items that match category token
	 * @param token category token
	 * @return a CompletableFuture containing the SearchResponse
	 */
	public CompletableFuture<SearchResponse<CatalogItem>> findCatalogItemByCategory(String token) {
		LOG.info("findCatalogItemByCategory async query: match(category.category_name, {})", token);
		return getAsyncClient().search(s -> s
				.index(getIndex())
				.query(q -> q
						.match(m -> m
								.field("category.category_name")
								.query(token)
						)
				),
				CatalogItem.class
		);
	}
	
	/**
	 * Updates document in Elasticsearch asynchronously
	 * @param item document to update
	 * @return a CompletableFuture containing update response
	 */
	public CompletableFuture<UpdateResponse<CatalogItem>> updateCatalogItem(CatalogItem item) {
		return getAsyncClient().update(u -> u
				.index(getIndex())
				.id(String.valueOf(item.getId()))
				.doc(item),
				CatalogItem.class
		);
	}
	
	/**
	 * Updates just the description field of a document based on id asynchronously
	 * @param id id of the document
	 * @param description new description
	 * @return a CompletableFuture containing update response
	 */
	public CompletableFuture<UpdateResponse<Map>> updateCatalogItemDescription(Integer id, String description) {
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("description", description);
		return getAsyncClient().update(u -> u
				.index(getIndex())
				.id(String.valueOf(id))
				.doc(jsonMap),
				Map.class
		);
	}
	
	/**
	 * Deletes a document from an index asynchronously
	 * @param id id of a document to be deleted
	 * @return a CompletableFuture containing delete response
	 */
	public CompletableFuture<DeleteResponse> deleteCatalogItem(Integer id) {
		return getAsyncClient().delete(d -> d
				.index(getIndex())
				.id(String.valueOf(id))
		);
	}

	/**
	 * Extracts catalog items from SearchResponse hits
	 */
	public List<CatalogItem> extractSearchResults(SearchResponse<CatalogItem> response) {
		if (response == null || response.hits() == null) {
			return Collections.emptyList();
		}
		return response.hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
