package com.harish.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class JwtGatewayService {

    @Value("${jwt.secretKey}")
    private String secretKey;

    public void validateToken(String token) {

        log.debug("Gateway JWT secret key (first 10 chars): {}", secretKey != null ? secretKey.substring(0, Math.min(10, secretKey.length())) : "NULL");
        log.debug("Token to validate (first 20 chars): {}", token != null ? token.substring(0, Math.min(20, token.length())) : "NULL");
        log.debug("Secret key length: {}, Token length: {}", secretKey != null ? secretKey.length() : 0, token != null ? token.length() : 0);

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

    }

}
