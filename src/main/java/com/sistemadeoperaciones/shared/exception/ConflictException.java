package com.sistemadeoperaciones.shared.exception;

/**
 * Conflicto de estado: la petición es válida, pero el recurso no se encuentra
 * en una situación que permita ejecutarla (por ejemplo, eliminar una operación
 * que ya dejó de estar pendiente de validación).
 *
 * Se mapea a 409 en {@link GlobalExceptionHandler}; sin esta subclase,
 * {@link BusinessException} respondería 400.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message);
    }
}
