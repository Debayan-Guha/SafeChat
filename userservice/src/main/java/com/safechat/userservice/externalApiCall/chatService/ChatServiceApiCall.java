package com.safechat.userservice.externalApiCall.chatService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.safechat.userservice.exception.ApplicationException.ExternalApiException;
import com.safechat.userservice.utility.api.ApiResponseFormatter;

import java.time.Duration;
import java.util.List;

@Component
public class ChatServiceApiCall {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceApiCall.class);

    private final WebClient webClient;
    private final String chatServiceUrl;
    private final String apiKeyToken;
    private final String serviceName = "USER-SERVICE";

    public ChatServiceApiCall(WebClient.Builder webClientBuilder,
            @Value("${service.chat.url}") String chatServiceUrl,
            @Value("${service.chat.apikey}") String apiKeyToken) {
        this.webClient = webClientBuilder.baseUrl(chatServiceUrl).build();
        this.chatServiceUrl = chatServiceUrl;
        this.apiKeyToken = apiKeyToken;
    }

    public ApiResponseFormatter<List<UserDeletionStatusDto>> sendUserDeletionBatch(List<String> userIds) {
        final String METHOD_NAME = "sendUserDeletionBatch";

        log.debug("{} - Sending user deletion batch for {} users", METHOD_NAME, userIds.size());

        ParameterizedTypeReference<ApiResponseFormatter<List<UserDeletionStatusDto>>> typeRef = new ParameterizedTypeReference<>() {
        };

        try {
            ApiResponseFormatter<List<UserDeletionStatusDto>> response = webClient.post()
                    .uri("/internal/user-deletion")
                    .header("serviceName", serviceName)
                    .header(HttpHeaders.AUTHORIZATION, "Service " + apiKeyToken)
                    .bodyValue(userIds)
                    .retrieve()
                    .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                        throw new ExternalApiException(
                                "Chat service server error: " + clientResponse.statusCode().value());
                    })
                    .bodyToMono(typeRef)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                throw new ExternalApiException("Chat service returned null response");
            }

            log.info("{} - User deletion batch completed successfully", METHOD_NAME);
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("{} - Authentication failed: {}", METHOD_NAME, e.getMessage());
            throw new ExternalApiException("Chat service authentication failed: Invalid API key");

        } catch (WebClientResponseException.Forbidden e) {
            log.error("{} - Authorization failed: {}", METHOD_NAME, e.getMessage());
            throw new ExternalApiException("Chat service authorization failed: Service not authorized");

        }
    }

}