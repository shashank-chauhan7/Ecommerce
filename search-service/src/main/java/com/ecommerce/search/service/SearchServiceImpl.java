package com.ecommerce.search.service;

import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.search.dto.ProductSearchResult;
import com.ecommerce.search.model.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PagedResponse<ProductSearchResult> search(String query, int page, int size) {
        log.info("Searching products with query='{}', page={}, size={}", query, page, size);

        String cacheKey = "search:query:" + query + ":page:" + page + ":size:" + size;
        @SuppressWarnings("unchecked")
        PagedResponse<ProductSearchResult> cached =
                (PagedResponse<ProductSearchResult>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for query='{}'", query);
            return cached;
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .should(s -> s.multiMatch(mm -> mm
                                .query(query)
                                .fields("name^3", "description", "brand^2")
                                .fuzziness("AUTO")
                        ))
                ))
                .withFilter(f -> f.term(t -> t.field("active").value(true)))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        List<ProductSearchResult> results = hits.getSearchHits().stream()
                .map(this::toSearchResult)
                .toList();

        PagedResponse<ProductSearchResult> response = PagedResponse.of(
                results, page, size, hits.getTotalHits());

        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    @Override
    public PagedResponse<ProductSearchResult> searchByCategory(String categoryName, int page, int size) {
        log.info("Searching products by category='{}', page={}, size={}", categoryName, page, size);

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("categoryName").value(categoryName)))
                        .must(m -> m.term(t -> t.field("active").value(true)))
                ))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        List<ProductSearchResult> results = hits.getSearchHits().stream()
                .map(this::toSearchResult)
                .toList();

        return PagedResponse.of(results, page, size, hits.getTotalHits());
    }

    private ProductSearchResult toSearchResult(SearchHit<ProductDocument> hit) {
        ProductDocument doc = hit.getContent();
        return new ProductSearchResult(
                doc.getId(),
                doc.getName(),
                doc.getDescription(),
                doc.getBrand(),
                doc.getCategoryName(),
                doc.getPrice(),
                doc.getImageUrl(),
                hit.getScore()
        );
    }
}
