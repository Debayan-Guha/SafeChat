package com.safechat.userservice.utility.api;

import java.sql.Timestamp;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL) 
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApiResponseFormatter<D> {

    private int statusCode;
    private String message;
    private D data;
    private PaginationData paginationData;
    private Timestamp timestamp;


    public static  ApiResponseFormatter<Void> formatter(int statusCode,String message){
        return new ApiResponseFormatter<>(statusCode,message,null,null,Timestamp.from(Instant.now()));
    }

    public static <D> ApiResponseFormatter<D> formatter(int statusCode,String message,D data){
        return new ApiResponseFormatter<>(statusCode,message,data,null,Timestamp.from(Instant.now()));
    }

    public static <D> ApiResponseFormatter<D> formatter(int statusCode,String message,D data,PaginationData paginationData){
        return new ApiResponseFormatter<>(statusCode,message,data,paginationData,Timestamp.from(Instant.now()));
    }

}
