package elastic.recipes;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import elastic.recipes.model.Recipe;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;

public class QueryES {
	static Logger LOG = LogManager.getLogger(QueryES.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final String INDEX_NAME = "recipes";

	public static void main(String[] args) {
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
			List<Recipe> recipes = findRecipeByIngredient(client, "chicken");
			LOG.info("Found {} recipes by ingredient: {}", recipes.size(), recipes);
		} catch (IOException e) {
			LOG.error("Error while accessing ES", e);
		}
	}

	public static List<Recipe> findRecipeByIngredient(RestHighLevelClient restClient, String ingredient) {
		try {
			SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("ingredients", ingredient);
			searchSourceBuilder.query(matchQueryBuilder);
			searchRequest.source(searchSourceBuilder);

			System.out.println("findRecipeByIngredient query: " + matchQueryBuilder.toString());

			SearchResponse response = restClient.search(searchRequest, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			List<Recipe> catalogItems = Arrays.stream(searchHits).map(e -> mapJsonToRecipe(e.getSourceAsString()))
					.filter(Objects::nonNull).collect(Collectors.toList());

			return catalogItems;
		} catch (IOException ex) {
			LOG.warn("Could not post {} to ES", ingredient, ex);
		}
		return Collections.emptyList();

	}

	protected static Recipe mapJsonToRecipe(String json) {
		try {
			return objectMapper.readValue(json, Recipe.class);
		} catch (IOException e) {
			LOG.warn("Could not convert {} to Recipe", e.getMessage());
		}
		return null;
	}
}
