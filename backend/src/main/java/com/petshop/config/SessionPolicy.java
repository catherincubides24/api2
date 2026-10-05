package com.petshop.config;

public enum SessionPolicy {
    /** Rechaza un nuevo login mientras exista una sesión activa en otro navegador. */
    BLOCK,
    /** Permite el nuevo login e invalida la sesión anterior. */
    REPLACE
}