package elasticsearch.experiments;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import elasticsearch.experiments.model.CatalogItem;

/**
 * Example of high-level synchronous client CRUD methods
 */
public class CrudMethodsSynchronous implements ClientCrudMethods {
	private static final Logger LOG = LogManager.getLogger(CrudMethodsSynchronous.class);
	private final String index;
	private final ElasticsearchClient client;
	private final ObjectMapper objectMapper;
	
	public CrudMethodsSynchronous(String index, ElasticsearchClient client) {
		this.index = index;
		this.client = client;
		this.objectMapper = new ObjectMapper();
	}
	
	protected String getIndex() {
		return index;
	}

	protected ElasticsearchClient getClient() {
		return client;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void createCatalogItem(List<CatalogItem> items) {
		items.forEach(e -> {
			try {
				IndexResponse indexResponse = getClient().index(i -> i
						.index(getIndex())
						.id(String.valueOf(e.getId()))
						.document(e)
				);
				if (indexResponse.result() == Result.Created) {
					LOG.info("Added catalog item with id {} to ES index {}", e.getId(), indexResponse.index());
				} else if (indexResponse.result() == Result.Updated) {
					LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", e.getId(), indexResponse.index(), indexResponse.version());
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
		try {
			GetResponse<CatalogItem> response = getClient().get(g -> g
					.index(getIndex())
					.id(String.valueOf(idToFind)),
					CatalogItem.class
			);
			if (response.found() && response.source() != null) {
				return Optional.of(response.source());
			} else {
				return Optional.empty();
			}
		} catch (IOException ex) {
			LOG.warn("Could not get {} from ES", idToFind, ex);
		}
		return Optional.empty();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItem(String text) {
		try {
			LOG.info("findCatalogItem query: simple_query_string({})", text);
			SearchResponse<CatalogItem> response = getClient().search(s -> s
					.index(getIndex())
					.query(q -> q
							.simpleQueryString(sqs -> sqs
									.query(text)
							)
					),
					CatalogItem.class
			);

			return response.hits().hits().stream()
					.map(Hit::source)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			LOG.warn("Could not search {} in ES", text, ex);
		}
		return Collections.emptyList();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItemByCategory(String token) {
		try {
			LOG.info("findCatalogItemByCategory query: match(category.category_name, {})", token);
			SearchResponse<CatalogItem> response = getClient().search(s -> s
					.index(getIndex())
					.query(q -> q
							.match(m -> m
									.field("category.category_name")
									.query(token)
							)
					),
					CatalogItem.class
			);

			return response.hits().hits().stream()
					.map(Hit::source)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			LOG.warn("Could not search category {} in ES", token, ex);
		}
		return Collections.emptyList();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public List<CatalogItem> findCatalogItemByCategoryAndSort(String token) {
		try {
			LOG.info("findCatalogItemByCategoryAndSort query: match(category.category_name, {}) sort price asc", token);
			SearchResponse<CatalogItem> response = getClient().search(s -> s
					.index(getIndex())
					.query(q -> q
							.match(m -> m
									.field("category.category_name")
									.query(token)
							)
					)
					.sort(so -> so
							.field(f -> f
									.field("price")
									.order(SortOrder.Asc)
							)
					),
					CatalogItem.class
			);

			return response.hits().hits().stream()
					.map(Hit::source)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			LOG.warn("Could not search category {} sorted in ES", token, ex);
		}
		return Collections.emptyList();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void updateCatalogItem(CatalogItem item) {
		try {
			UpdateResponse<CatalogItem> response = getClient().update(u -> u
					.index(getIndex())
					.id(String.valueOf(item.getId()))
					.doc(item),
					CatalogItem.class
			);
			if (response.result() == Result.Created) {
				LOG.info("Added catalog item with id {} to ES index {}", item.getId(), response.index());
			} else if (response.result() == Result.Updated) {
				LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", item.getId(), response.index(), response.version());
			}
		} catch (IOException ex) {
			LOG.warn("Could not update {} in ES", item, ex);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void updateCatalogItemDescription(Integer id, String description) {
		try {
			Map<String, Object> docMap = new HashMap<>();
			docMap.put("description", description);
			UpdateResponse<Map> response = getClient().update(u -> u
					.index(getIndex())
					.id(String.valueOf(id))
					.doc(docMap),
					Map.class
			);
			if (response.result() == Result.Created) {
				LOG.info("Added catalog item with id {} to ES index {}", id, response.index());
			} else if (response.result() == Result.Updated) {
				LOG.info("Updated catalog item with id {} to ES index {}, version of the object is {}", id, response.index(), response.version());
			}
		} catch (IOException ex) {
			LOG.warn("Could not update catalog item description for id {} in ES", id, ex);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteCatalogItem(Integer id) {
		try {
			DeleteResponse response = getClient().delete(d -> d
					.index(getIndex())
					.id(String.valueOf(id))
			);
			if (response.result() == Result.NotFound) {
				LOG.info("Could not find catalog item with id {} in ES index {}", id, response.index());
			} else if (response.result() == Result.Deleted) {
				LOG.info("Found and deleted");
			}
		} catch (IOException ex) {
			LOG.warn("Could not delete catalog item with id {} from ES", id, ex);
		}
	}
}
