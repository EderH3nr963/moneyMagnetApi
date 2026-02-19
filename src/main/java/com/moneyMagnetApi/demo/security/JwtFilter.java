package com.moneyMagnetApi.demo.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private TokenService tokenService;
    private UsuarioRepository usuarioRepository;

    public JwtFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = recoverToken(request);

        if (token != null) {
            try {
                UUID id = tokenService.validateToken(token);

                Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

                if (usuarioOpt.isPresent()) {
                    UsuarioDetailsImpl usuarioDetails =
                            new UsuarioDetailsImpl(usuarioOpt.get());

                    var auth = new UsernamePasswordAuthenticationToken(
                            usuarioDetails,
                            null,
                            usuarioDetails.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JWTVerificationException ex) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");

                String body = """
            {
              "status": 401,
              "error": "UNAUTHORIZED",
              "message": "Token inválido ou expirado.",
              "timestamp": "%s"
            }
            """.formatted(java.time.Instant.now());

                response.getWriter().write(body);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("authorization");
        if (authHeader == null) return null;
        return authHeader.substring(7);
    }
}
