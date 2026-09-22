package com.petshop.exception;

/** Se lanza cuando un usuario autenticado intenta acceder a informacion que no le pertenece. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
