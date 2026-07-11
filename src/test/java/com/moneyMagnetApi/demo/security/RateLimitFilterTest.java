package com.moneyMagnetApi.demo.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RateLimitFilterTest {

    @Test
    void ignoresSpoofedForwardedIpWhenApplyingAuthLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 0; attempt < 7; attempt++) {
            MockHttpServletRequest request = authRequest("203.0.113." + attempt);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(authRequest("198.51.100.200"), blockedResponse, chain);

        assertEquals(429, blockedResponse.getStatus());
    }

    private MockHttpServletRequest authRequest(String spoofedIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", spoofedIp);
        return request;
    }
}
