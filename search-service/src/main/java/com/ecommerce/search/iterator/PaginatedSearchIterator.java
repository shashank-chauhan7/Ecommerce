package com.ecommerce.search.iterator;

import com.ecommerce.search.model.ProductDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class PaginatedSearchIterator implements Iterator<List<ProductDocument>> {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NativeQuery baseQuery;
    private final int pageSize;

    private int currentPage;
    private long totalHits;
    private boolean initialized;

    public PaginatedSearchIterator(ElasticsearchOperations elasticsearchOperations,
                                   NativeQuery baseQuery, int pageSize) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.baseQuery = baseQuery;
        this.pageSize = pageSize;
        this.currentPage = 0;
        this.totalHits = -1;
        this.initialized = false;
    }

    @Override
    public boolean hasNext() {
        if (!initialized) {
            return true;
        }
        return (long) currentPage * pageSize < totalHits;
    }

    @Override
    public List<ProductDocument> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more pages available");
        }

        NativeQuery pagedQuery = NativeQuery.builder()
                .withQuery(baseQuery.getQuery())
                .withPageable(PageRequest.of(currentPage, pageSize))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(pagedQuery, ProductDocument.class);

        if (!initialized) {
            totalHits = hits.getTotalHits();
            initialized = true;
        }

        currentPage++;

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
