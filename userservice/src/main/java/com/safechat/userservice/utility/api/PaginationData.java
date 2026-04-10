package com.safechat.userservice.utility.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginationData {
    
    private long totalPages;
    private long totalElements;
    private long currentPageTotalElements;
    private long currentPage;
}
