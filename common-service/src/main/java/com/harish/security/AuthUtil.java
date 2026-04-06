package com.harish.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

@Slf4j
@Component
public class AuthUtil {

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {

        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));

    }

    public String generateAccessToken(JwtUserPrincipal user) {

        log.debug("AuthUtil JWT secret key (first 10 chars): {}", jwtSecretKey != null ? jwtSecretKey.substring(0, Math.min(10, jwtSecretKey.length())) : "NULL");
        log.debug("AuthUtil secret key length: {}", jwtSecretKey != null ? jwtSecretKey.length() : 0);

        String token = Jwts.builder()
                .subject(user.username())
                .claim("userId", user.userId().toString())
                .claim("name", user.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000*60*100))
                .signWith(getSecretKey())
                .compact();

        log.debug("Generated token (first 20 chars): {}", token.substring(0, Math.min(20, token.length())));
        return token;

    }

    public JwtUserPrincipal verifyAccessToken(String token) {

        log.debug("AuthUtil verify JWT secret key (first 10 chars): {}", jwtSecretKey != null ? jwtSecretKey.substring(0, Math.min(10, jwtSecretKey.length())) : "NULL");
        log.debug("AuthUtil verify secret key length: {}", jwtSecretKey != null ? jwtSecretKey.length() : 0);

        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.parseLong(claims.get("userId", String.class));
        String name = claims.get("name", String.class);
        String username = claims.getSubject();
        return new JwtUserPrincipal(userId, name, username, null, new ArrayList<>());

    }

    public Long getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal userPrincipal)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT Found");
        }
        assert userPrincipal != null;
        return userPrincipal.userId();

    }

}