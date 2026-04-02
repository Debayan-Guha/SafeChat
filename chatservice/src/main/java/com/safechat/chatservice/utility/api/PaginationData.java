package com.safechat.chatservice.utility.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginationData {
    
    private long totalPages;
    private long totalElements;
    private long currentPageTotalElements;
    private long currentPage;
}
