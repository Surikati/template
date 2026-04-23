package cz.komercpoj.tmpmgmt.search.application;

import cz.komercpoj.tmpmgmt.search.api.dto.SearchHit;
import cz.komercpoj.tmpmgmt.search.config.OpenSearchConfig;
import cz.komercpoj.tmpmgmt.search.domain.SearchableDocument;
import java.util.List;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final int MAX_LIMIT = 200;

    private final OpenSearchClient client;

    public SearchService(OpenSearchClient client) {
        this.client = client;
    }

    public List<SearchHit> search(String query, String type, int limit) {
        int capped = Math.min(Math.max(1, limit), MAX_LIMIT);
        List<String> indices = resolveIndices(type);

        Query q = Query.of(qb -> qb.multiMatch(MultiMatchQuery.of(mm -> mm
                .query(query == null ? "" : query)
                .fields("name^2", "description", "slug"))));

        try {
            SearchResponse<SearchableDocument> resp = client.search(
                    SearchRequest.of(r -> r
                            .index(indices)
                            .query(q)
                            .size(capped)),
                    SearchableDocument.class);

            return resp.hits().hits().stream()
                    .map(h -> new SearchHit(
                            h.source() == null ? null : h.source().id(),
                            indexToType(h.index()),
                            h.source() == null ? null : h.source().slug(),
                            h.source() == null ? null : h.source().name(),
                            h.source() == null ? null : h.source().description(),
                            h.source() == null ? null : h.source().category(),
                            h.source() == null ? null : h.source().status(),
                            h.source() == null ? null : h.source().updatedAt(),
                            h.score() == null ? 0.0 : h.score()))
                    .toList();
        } catch (Exception ex) {
            throw new RuntimeException("Search failed: " + ex.getMessage(), ex);
        }
    }

    private static List<String> resolveIndices(String type) {
        if (type == null || "all".equalsIgnoreCase(type)) {
            return List.of(OpenSearchConfig.INDEX_TEMPLATE, OpenSearchConfig.INDEX_CLAUSE);
        }
        return switch (type.toLowerCase()) {
            case "template" -> List.of(OpenSearchConfig.INDEX_TEMPLATE);
            case "clause" -> List.of(OpenSearchConfig.INDEX_CLAUSE);
            default -> List.of(OpenSearchConfig.INDEX_TEMPLATE, OpenSearchConfig.INDEX_CLAUSE);
        };
    }

    private static String indexToType(String indexName) {
        if (OpenSearchConfig.INDEX_TEMPLATE.equals(indexName)) return "template";
        if (OpenSearchConfig.INDEX_CLAUSE.equals(indexName)) return "clause";
        return indexName;
    }
}
