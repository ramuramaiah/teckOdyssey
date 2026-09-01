package elastic.recipes;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import elastic.recipes.model.Recipe;

public class QueryES {

    private static final Logger LOG = LogManager.getLogger(QueryES.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INDEX_NAME = "recipes";


    public static void main(String[] args) {

        try (Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "localhost", 9200)
        ).build()) {

            Rest5ClientTransport transport =
                    new Rest5ClientTransport(
                            restClient,
                            new JacksonJsonpMapper(OBJECT_MAPPER)
                    );

            ElasticsearchClient client = new ElasticsearchClient(transport);

            List<Recipe> recipes = findRecipeByIngredient(client, "chicken");
            LOG.info("Found {} recipes by ingredient: {}", recipes.size(), recipes);

        } catch (IOException e) {
            LOG.error("Error while accessing ES", e);
        }
    }


    /**
     * Searches for recipes that match the given ingredient
     * using a match query on the "ingredients" field.
     */
    public static List<Recipe> findRecipeByIngredient(
            ElasticsearchClient client,
            String ingredient) {

        try {
            SearchResponse<Recipe> response =
                    client.search(s -> s
                                    .index(INDEX_NAME)
                                    .query(q -> q
                                            .match(m -> m
                                                    .field("ingredients")
                                                    .query(ingredient)
                                            )
                                    ),
                            Recipe.class
                    );

            LOG.info(
                    "findRecipeByIngredient query: match(ingredients, {})",
                    ingredient
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException ex) {
            LOG.warn("Could not query ES for ingredient {}", ingredient, ex);
        }

        return Collections.emptyList();
    }
}
