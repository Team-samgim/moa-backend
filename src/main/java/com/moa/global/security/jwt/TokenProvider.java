package com.moa.global.security.jwt;

import com.moa.api.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.SecretKey;            // 또는 java.security.Key
import java.util.Date;

import io.jsonwebtoken.io.Decoders;
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

        byte[] keyBytes = Decoders.BASE64.decode(props.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, String loginId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(loginId)
                .claim("loginId", loginId)
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

    public String createRefreshToken(String loginId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(loginId)
                .claim("loginId", loginId)
                .claim("role", role.name())
                .claim("tokenType", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(props.getRefreshTokenTtl())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}