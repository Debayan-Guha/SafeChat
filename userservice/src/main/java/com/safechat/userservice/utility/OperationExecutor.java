package com.safechat.userservice.utility;

import java.util.function.Supplier;

import com.safechat.userservice.exception.ApplicationException.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

    public static <T> T redisGet(Supplier<T> action, String serviceName, String methodName) {
        try {
            return action.get();
        } catch (Exception RedisEx) {
            logger.warn("Redis GET failed: {}\tService={}\tMethod={}",
                    RedisEx.getMessage(), serviceName, methodName);
        }
        return null;
    }

    public static void redisSave(Runnable action, String serviceName, String methodName) {
        try {
            action.run();
        } catch (Exception RedisEx) {
            logger.warn("Redis SAVE failed: {}\tService={}\tMethod={}",
                    RedisEx.getMessage(), serviceName, methodName);
        }
    }

    public static void redisRemove(Runnable action, String serviceName, String methodName) {
        try {
            action.run();
        } catch (Exception RedisEx) {
            logger.warn("Redis Remove  failed: {}\tService={}\tMethod={}",
                    RedisEx.getMessage(), serviceName, methodName);
        }
    }

    public static void dbSave(Runnable action, String serviceName, String methodName) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error("DB FAILED: {}\tService={}\tMethod={}",
                    e.getMessage(), serviceName, methodName); 
            throw new DatabaseException();           
        }
    }

    public static <T> T dbSaveAndReturn(Supplier<T> action, String serviceName, String methodName) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("DB FAILED: {}\tService={}\tMethod={}",
                    e.getMessage(), serviceName, methodName); 
            throw new DatabaseException();           
        }
    }

    public static <T> T dbGet(Supplier<T> action, String serviceName, String methodName) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("DB FAILED: {}\tService={}\tMethod={}",
                    e.getMessage(), serviceName, methodName);
            throw new DatabaseException();
        }
    }

    public static void dbRemove(Runnable action, String serviceName, String methodName) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error("DB FAILED: {}\tService={}\tMethod={}",
                    e.getMessage(), serviceName, methodName);
            throw new DatabaseException();
        }
    }

    public static <T> T map(Supplier<T> action, String serviceName, String methodName) {
        try {
            return action.get();
        } catch (Exception MapEx) {
            logger.error("dto mapping failed:{}\tService={}\tMethod={}", MapEx.getMessage(), serviceName,
                    methodName);
            throw new DataProcessingException();
        }
    }

}
