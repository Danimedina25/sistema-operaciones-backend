package com.sistemadeliberacion.shared.exception;

import com.sistemadeliberacion.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.sistemadeliberacion.auth.exceptions.*;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================================
    // JSON MAL FORMADO
    // ==========================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonParseException(HttpMessageNotReadableException ex) {
        return buildErrorResponse("Error al leer el JSON: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ==========================================================
    // VALIDACIÓN @Valid
    // ==========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for (var error : ex.getBindingResult().getAllErrors()) {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        }

        ApiResponse<Object> response = new ApiResponse<>(
                false,
                "Error de validación",
                null,
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ==========================================================
    // AUTH
    // ==========================================================
/*    @ExceptionHandler({
            UsuarioNoEncontradoException.class,
            CredencialesInvalidasException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleAuthExceptions(RuntimeException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsuarioBloqueado(UsuarioBloqueadoException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("error", ex.getMessage());
        details.put("bloqueado", true);

        ApiResponse<Object> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                null,
                details
        );

        return new ResponseEntity<>(response, HttpStatus.LOCKED);
    }

    // ==========================================================
    // PASSWORD RECOVERY
    // ==========================================================
    @ExceptionHandler({
            TokenInvalidoException.class,
            TokenExpiradoException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleTokenExceptions(RuntimeException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ==========================================================
    // PROFILE
    // ==========================================================
    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleProfileNotFound(ProfileNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProfileUpdateException.class)
    public ResponseEntity<ApiResponse<Object>> handleProfileUpdate(ProfileUpdateException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailInUse(EmailInUseException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.IM_USED);
    }

    // ==========================================================
    // ROLES
    // ==========================================================
    @ExceptionHandler(MaxRolesDiariosExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxRolesExceeded(MaxRolesDiariosExceededException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ==========================================================
    // ResponseStatusException — SE USA PARA TOKENS
    // ==========================================================
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse(ex.getReason(), HttpStatus.CONFLICT);
    }

    // ==========================================================
    // REFERRAL CODES
    // ==========================================================
    @ExceptionHandler(InvalidReferralCodeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidReferralCode(
            InvalidReferralCodeException ex
    ) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }*/

    // ==========================================================
    // EXCEPCIÓN GENERAL
    // ==========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiResponse<Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenInvalido(TokenInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenExpiradoException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenExpirado(TokenExpiradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }
    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);

        ApiResponse<Object> response = new ApiResponse<>(
                false,
                message,
                null,
                error
        );

        return new ResponseEntity<>(response, status);
    }
}
