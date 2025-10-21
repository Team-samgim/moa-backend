package com.moa.global.security.jwt;

import com.moa.api.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.SecretKey;            // 또는 java.security.Key
import java.util.Date;

import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class TokenProvider {
    private final JwtProperties props;
    private final javax.crypto.SecretKey key; // 또는 java.security.Key

    public TokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(email)
                .claim("uid", userId)
                .claim("role", role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(props.getAccessTokenTtl())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}