package com.petshop.exception;

/** Se lanza cuando el usuario ya tiene una sesión activa en otro navegador. */
public class SessionConflictException extends RuntimeException {

    public SessionConflictException(String message) {
        super(message);
    }
}