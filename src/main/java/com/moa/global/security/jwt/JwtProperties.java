/**
 * 작성자: 정소영
 */
package com.moa.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtProperties {
    private String secret;
    private String header = "Authorization";
    private String prefix = "Bearer ";
    private long accessTokenTtl;
    private long refreshTokenTtl;
}