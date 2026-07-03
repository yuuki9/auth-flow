package com.auth.practice.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiryMs;
    private long refreshTokenExpiryMs;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiryMs() { return accessTokenExpiryMs; }
    public void setAccessTokenExpiryMs(long ms) { this.accessTokenExpiryMs = ms; }
    public long getRefreshTokenExpiryMs() { return refreshTokenExpiryMs; }
    public void setRefreshTokenExpiryMs(long ms) { this.refreshTokenExpiryMs = ms; }
}
