package com.lexoft.rag.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Component
public class McpJwtRequestCustomizer implements McpSyncHttpClientRequestCustomizer {

    @Override
    public void customize(HttpRequest.Builder requestBuilder, String method, URI uri, String body, McpTransportContext context) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AbstractOAuth2TokenAuthenticationToken<?> oauthAuth) {
            requestBuilder.header("Authorization", "Bearer " + oauthAuth.getToken().getTokenValue());
        }
    }
}
