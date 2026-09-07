package elasticsearch.experiments;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import elasticsearch.experiments.model.CatalogItem;

public class QueryES {

    private static final Logger LOG = LogManager.getLogger(QueryES.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SHAKESPEARE_INDEX = "shakespeare";

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

            // Query Shakespeare Lines
            List<LineFromShakespeare> lines = findShakespeareLinesBySpeaker(client, "KING");
            LOG.info("Found {} Shakespeare lines by speaker: {}", lines.size(), lines);

        } catch (IOException e) {
            LOG.error("Error while accessing ES", e);
        }
    }

    /**
     * Searches for lines from Shakespeare that match the given speaker name.
     */
    public static List<LineFromShakespeare> findShakespeareLinesBySpeaker(
            ElasticsearchClient client,
            String speaker) {

        try {
            SearchResponse<LineFromShakespeare> response =
                    client.search(s -> s
                                    .index(SHAKESPEARE_INDEX)
                                    .query(q -> q
                                            .match(m -> m
                                                    .field("speaker")
                                                    .query(speaker)
                                            )
                                    ),
                            LineFromShakespeare.class
                    );

            LOG.info("findShakespeareLinesBySpeaker query: match(speaker, {})", speaker);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (IOException ex) {
            LOG.warn("Could not query ES for speaker {}", speaker, ex);
        }

        return Collections.emptyList();
    }
}
