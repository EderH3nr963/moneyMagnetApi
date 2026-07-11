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
                    .withClaim("tokenVersion", usuarioDetails.getUsuario().getTokenVersion())
                    .withExpiresAt(expiration) // 48h
                    .withIssuer("api")
                    .sign(algorithm);

            return token;
        } catch(JWTCreationException exception) {
            return null;
        }
    }

    public ValidatedToken validateToken(String token) {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var decodedToken = JWT
                    .require(algorithm)
                    .withIssuer("api")
                    .build()
                    .verify(token);

            Long tokenVersion = decodedToken.getClaim("tokenVersion").asLong();
            if (tokenVersion == null) {
                throw new JWTVerificationException("Token sem versao de sessao.");
            }

            return new ValidatedToken(
                    UUID.fromString(decodedToken.getSubject()),
                    tokenVersion
            );
    }

    public record ValidatedToken(UUID userId, long tokenVersion) {}
}
