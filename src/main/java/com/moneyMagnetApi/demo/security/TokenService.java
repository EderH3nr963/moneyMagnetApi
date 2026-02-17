package com.moneyMagnetApi.demo.security;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(UsuarioDetailsImpl usuarioDetails, Instant expiration) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withSubject(usuarioDetails.getId().toString())
                    .withExpiresAt(expiration) // 48h
                    .withIssuer("api")
                    .sign(algorithm);

            return token;
        } catch(JWTCreationException exception) {
            return null;
        }
    }

    public UUID validateToken(String token) {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String id = JWT
                    .require(algorithm)
                    .withIssuer("api")
                    .build()
                    .verify(token)
                    .getSubject();

            return UUID.fromString(id);
    }
}
