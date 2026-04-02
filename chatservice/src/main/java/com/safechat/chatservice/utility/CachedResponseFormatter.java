package com.safechat.chatservice.utility;

import java.time.Instant;

import com.safechat.chatservice.utility.api.PaginationData;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor@Getter@Setter
public class CachedResponseFormatter<D> {

    private D data;
    private PaginationData paginationData;
    private Instant cachedAt;

    public static <D,M> CachedResponseFormatter<D> formatter(D data,PaginationData paginationData)
    {
        return new CachedResponseFormatter<>(data,paginationData,Instant.now());
    }
    public static <D,M> CachedResponseFormatter<D> formatter(D data)
    {
        return new CachedResponseFormatter<>(data,null,Instant.now());
    }
}