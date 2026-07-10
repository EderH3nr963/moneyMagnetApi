package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountsResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectTokenResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyItemResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PluggyClient {

    private static final String API_KEY_CACHE_KEY = "pluggy-api-key";

    private final RestClient restClient;
    private final Cache<String, String> apiKeyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(110))
            .maximumSize(1)
            .build();

    @Value("${pluggy.api-key}")
    private String configuredApiKey;

    @Value("${pluggy.client-id}")
    private String clientId;

    @Value("${pluggy.client-secret}")
    private String clientSecret;

    public List<PluggyAccountResponse> getAccounts(String itemId) {
        try {
            return fetchAccounts(itemId, getCachedOrConfiguredApiKey());
        } catch (RestClientResponseException exception) {
            if (!isInvalidApiKey(exception)) {
                throw exception;
            }

            apiKeyCache.invalidate(API_KEY_CACHE_KEY);
            return fetchAccounts(itemId, fetchAndCacheApiKey());
        }
    }

    public PluggyConnectTokenResponse createConnectToken(UUID usuarioId) {
        try {
            return fetchConnectToken(usuarioId, getCachedOrConfiguredApiKey());
        } catch (RestClientResponseException exception) {
            if (!isInvalidApiKey(exception)) {
                throw exception;
            }

            apiKeyCache.invalidate(API_KEY_CACHE_KEY);
            return fetchConnectToken(usuarioId, fetchAndCacheApiKey());
        }
    }

    public PluggyItemResponse getItem(String itemId) {
        try {
            return fetchItem(itemId, getCachedOrConfiguredApiKey());
        } catch (RestClientResponseException exception) {
            if (!isInvalidApiKey(exception)) {
                throw exception;
            }

            apiKeyCache.invalidate(API_KEY_CACHE_KEY);
            return fetchItem(itemId, fetchAndCacheApiKey());
        }
    }

    public List<PluggyTransactionResponse> getTransactions(
            String accountId,
            LocalDateTime lastSync
    ) {
        try {
            return fetchTransactions(accountId, lastSync, getCachedOrConfiguredApiKey());
        } catch (RestClientResponseException exception) {
            if (!isInvalidApiKey(exception)) {
                throw exception;
            }

            apiKeyCache.invalidate(API_KEY_CACHE_KEY);
            
            return fetchTransactions(accountId, lastSync, fetchAndCacheApiKey());
        }
    }

    private List<PluggyAccountResponse> fetchAccounts(String itemId, String apiKey) {
        PluggyAccountsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts")
                        .queryParam("itemId", itemId)
                        .build())
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(PluggyAccountsResponse.class);

        return response != null && response.results() != null
                ? response.results()
                : List.of();
    }

    private PluggyConnectTokenResponse fetchConnectToken(UUID usuarioId, String apiKey) {
        PluggyConnectTokenResponse response = restClient.post()
                .uri("/connect_token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", apiKey)
                .body(new PluggyConnectTokenRequest(
                        new PluggyConnectTokenOptions(usuarioId.toString(), false)
                ))
                .retrieve()
                .body(PluggyConnectTokenResponse.class);

        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new IllegalStateException("A Pluggy nao retornou um Connect Token valido.");
        }

        return response;
    }

    private PluggyItemResponse fetchItem(String itemId, String apiKey) {
        PluggyItemResponse response = restClient.get()
                .uri("/items/{itemId}", itemId)
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(PluggyItemResponse.class);

        if (response == null || !StringUtils.hasText(response.id())) {
            throw new IllegalStateException("A Pluggy nao retornou um Item valido.");
        }

        return response;
    }

    private List<PluggyTransactionResponse> fetchTransactions(
            String accountId,
            LocalDateTime lastSync,
            String apiKey
    ) {
        List<PluggyTransactionResponse> transactions = new ArrayList<>();
        Set<String> visitedCursors = new HashSet<>();
        String after = null;

        do {
            PluggyTransactionsResponse response = fetchTransactionsPage(
                    accountId,
                    lastSync,
                    after,
                    apiKey
            );

            if (response == null) {
                break;
            }
            if (response.results() != null) {
                transactions.addAll(response.results());
            }

            after = extractAfterCursor(response.next());
        } while (StringUtils.hasText(after) && visitedCursors.add(after));

        return transactions;
    }

    private PluggyTransactionsResponse fetchTransactionsPage(
            String accountId,
            LocalDateTime lastSync,
            String after,
            String apiKey
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder
                            .path("/v2/transactions")
                            .queryParam("accountId", accountId);

                    if (StringUtils.hasText(after)) {
                        uri.queryParam("after", after);
                    } else if (lastSync != null) {
                        uri.queryParam("dateFrom", lastSync.toLocalDate());
                    }

                    return uri.build();
                })
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(PluggyTransactionsResponse.class);
    }

    private String extractAfterCursor(String next) {
        if (!StringUtils.hasText(next)) {
            return null;
        }

        return UriComponentsBuilder.fromUriString(next)
                .build()
                .getQueryParams()
                .getFirst("after");
    }

    private String getCachedOrConfiguredApiKey() {
        String cachedApiKey = apiKeyCache.getIfPresent(API_KEY_CACHE_KEY);

        if (StringUtils.hasText(cachedApiKey)) {
            return cachedApiKey;
        }
        if (StringUtils.hasText(configuredApiKey)) {
            apiKeyCache.put(API_KEY_CACHE_KEY, configuredApiKey);
            return configuredApiKey;
        }

        return fetchAndCacheApiKey();
    }

    private synchronized String fetchAndCacheApiKey() {
        String cachedApiKey = apiKeyCache.getIfPresent(API_KEY_CACHE_KEY);

        if (StringUtils.hasText(cachedApiKey)) {
            return cachedApiKey;
        }
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException("Credenciais da Pluggy nao configuradas.");
        }

        PluggyAuthResponse response = restClient.post()
                .uri("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PluggyAuthRequest(clientId, clientSecret))
                .retrieve()
                .body(PluggyAuthResponse.class);

        if (response == null || !StringUtils.hasText(response.apiKey())) {
            throw new IllegalStateException("A Pluggy nao retornou uma apiKey valida.");
        }

        apiKeyCache.put(API_KEY_CACHE_KEY, response.apiKey());
        return response.apiKey();
    }

    private boolean isInvalidApiKey(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 401
                || exception.getStatusCode().value() == 403;
    }

    private record PluggyAuthRequest(String clientId, String clientSecret) {
    }

    private record PluggyAuthResponse(String apiKey) {
    }

    private record PluggyConnectTokenRequest(PluggyConnectTokenOptions options) {
    }

    private record PluggyConnectTokenOptions(
            String clientUserId,
            boolean avoidDuplicates
    ) {
    }
}
