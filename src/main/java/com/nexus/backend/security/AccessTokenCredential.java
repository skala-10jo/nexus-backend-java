package com.nexus.backend.security;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Custom TokenCredential implementation that wraps an existing access token
 * for use with Microsoft Graph SDK.
 */
public class AccessTokenCredential implements TokenCredential {

    private final String accessToken;
    private final OffsetDateTime expiresAt;

    public AccessTokenCredential(String accessToken, OffsetDateTime expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        AccessToken token = new AccessToken(accessToken, expiresAt);
        return Mono.just(token);
    }
}
