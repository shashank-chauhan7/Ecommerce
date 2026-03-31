package com.ecommerce.search.service;

import com.ecommerce.search.model.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void indexProduct(ProductDocument product) {
        log.info("Indexing product id={}", product.getId());
        elasticsearchOperations.save(product);
    }

    public void deleteProduct(String id) {
        log.info("Deleting product id={} from index", id);
        elasticsearchOperations.delete(id, ProductDocument.class);
    }

    public void bulkIndex(List<ProductDocument> products) {
        log.info("Bulk indexing {} products", products.size());
        elasticsearchOperations.save(products);
    }
}
