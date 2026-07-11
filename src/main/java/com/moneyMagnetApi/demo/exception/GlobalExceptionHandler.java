package com.moneyMagnetApi.demo.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.moneyMagnetApi.demo.dto.usuario.response.ApiError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                safeRequestPath(request),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(error);
    }

    private String safeRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String resetPasswordPrefix = "/api/v1/auth/reset-password/";

        if (path.startsWith(resetPasswordPrefix)) {
            return resetPasswordPrefix + "{token}";
        }

        return path;
    }

    // 404 - Entidade não encontrada
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 400 - Erro de validação (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 400 - ConstraintViolation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        LOGGER.debug("Violacao de restricao na requisicao {}", safeRequestPath(request), ex);
        return buildError(HttpStatus.BAD_REQUEST, "Parametros invalidos", request);
    }

    // 403 - Acesso negado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.FORBIDDEN, "Acesso negado", request);
    }

    // 500 - Erro ao enviar email
    @ExceptionHandler(MailSendException.class)
    public ResponseEntity<ApiError> handleMailError(
            MailSendException ex,
            HttpServletRequest request
    ) {
        LOGGER.error("Erro ao enviar email na requisicao {}", safeRequestPath(request), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro ao enviar email",
                request);
    }

    // 401 - Qualquer outra exception
    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ApiError> handleJWTVerification(
            JWTVerificationException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Token inválido ou expirado",
                request
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return buildError(
                ex.getStatus(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Email ou senha inválidos",
                request
        );
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ){
        LOGGER.debug("Corpo de requisicao invalido em {}", safeRequestPath(request), ex);
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido",
                request
        );
    }
    
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<ApiError> handleJsonParseException(
            JsonParseException ex,
            HttpServletRequest request
    ) {
        LOGGER.debug("JSON invalido em {}", safeRequestPath(request), ex);
        return buildError(
                HttpStatus.BAD_REQUEST,
                "JSON invalido",
                request
        );
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        LOGGER.debug("Argumento invalido em {}", safeRequestPath(request), ex);
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Argumento invalido",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        LOGGER.error("Erro interno nao tratado em {}", safeRequestPath(request), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor",
                request);
    }

}
