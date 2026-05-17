package com.OnRoot.onroot.global.security;

import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class JwtTokenProvider {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long validityMillis;

    public JwtTokenProvider(@Value("${jwt.secret:changeit}") String secret,
                            @Value("${jwt.expiration-ms:86400000}") long validityMillis) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.validityMillis = validityMillis;
    }

    public String createToken(Long userId, String email) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(validityMillis));
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("email", email)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public Long getUserId(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return Long.parseLong(jwt.getSubject());
    }

    public boolean validate(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
