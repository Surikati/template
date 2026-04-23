package cz.komercpoj.tmpmgmt.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchConfig.class);

    public static final String INDEX_TEMPLATE = "tmpmgmt-templates";
    public static final String INDEX_CLAUSE = "tmpmgmt-clauses";

    @Bean
    OpenSearchClient openSearchClient(OpenSearchProperties props, ObjectMapper mapper) {
        var transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(props.scheme(), props.host(), props.port()))
                .setMapper(new JacksonJsonpMapper(mapper))
                .build();
        return new OpenSearchClient(transport);
    }

    /** Creates indices with basic mappings on startup if they don't exist yet. Idempotent. */
    @Bean
    ApplicationRunner openSearchBootstrap(OpenSearchClient client) {
        return args -> {
            ensureIndex(client, INDEX_TEMPLATE);
            ensureIndex(client, INDEX_CLAUSE);
        };
    }

    private void ensureIndex(OpenSearchClient client, String index) throws IOException {
        boolean exists = client.indices()
                .exists(ExistsRequest.of(r -> r.index(index)))
                .value();
        if (exists) return;

        log.info("Creating OpenSearch index '{}'", index);
        client.indices().create(CreateIndexRequest.of(r -> r
                .index(index)
                .mappings(TypeMapping.of(t -> t
                        .properties("id",          Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("slug",        Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("name",        Property.of(p -> p.text(TextProperty.of(ts -> ts))))
                        .properties("description", Property.of(p -> p.text(TextProperty.of(ts -> ts))))
                        .properties("category",    Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("status",      Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        .properties("updatedAt",   Property.of(p -> p.date(d -> d)))))));
    }
}
