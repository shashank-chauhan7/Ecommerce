package com.ecommerce.search.service;

import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.search.dto.ProductSearchResult;

public interface SearchService {

    PagedResponse<ProductSearchResult> search(String query, int page, int size);

    PagedResponse<ProductSearchResult> searchByCategory(String categoryName, int page, int size);
}
