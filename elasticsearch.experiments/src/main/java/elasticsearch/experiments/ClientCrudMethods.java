package elasticsearch.experiments;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;

import elasticsearch.experiments.model.CatalogItem;

/**
 * Sample of CRUD methods for Elasticsearch
 *
 */
public interface ClientCrudMethods {

	/**
	 * Adds an items to Elasticsearch index
	 * @param items items to be added
	 */
	void createCatalogItem(List<CatalogItem> items);

	/**
	 * Returns an item in the index based on specified id
	 * @param idToFind id
	 * @returnan item in the index based on specified id
	 */
	Optional<CatalogItem> getCatalogItemById(Integer idToFind);

	/**
	 * Performs full text search on the index and returns items that matched
	 * @param text search text
	 * @return items that matched the search
	 */
	List<CatalogItem> findCatalogItem(String text);

	/**
	 * Returns items that match category token
	 * @param token category token
	 * @return items that match category token
	 */
	List<CatalogItem> findCatalogItemByCategory(String token);

	/**
	 * Returns items that match category token
	 * @param token category token
	 * @return items that match category token
	 */
	List<CatalogItem> findCatalogItemByCategoryAndSort(String token);
	
	/**
	 * Updates document in Elasticsearch
	 * @param item document to update
	 * @return update status
	 * @throws JsonProcessingException
	 */
	void updateCatalogItem(CatalogItem item);

	/**
	 * Updates just the description field of a document base on id
	 * @param id id of the document
	 * @param description new description
	 */
	void updateCatalogItemDescription(Integer id, String description);

	/**
	 * Deletes a document from an index
	 * @param id id of a document to be deleted
	 */
	void deleteCatalogItem(Integer id);

}