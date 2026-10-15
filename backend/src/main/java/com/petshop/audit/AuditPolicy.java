package com.petshop.audit;

import org.springframework.stereotype.Component;

/** Decide qué entidades son técnicas y no deben auditarse como cambio de datos. */
@Component
public class AuditPolicy {

    private static final String AUDIT_LOG = "AuditLog";
    private static final String USER_SESSION = "UserSession";

    /** Los logs no se auditan a sí mismos y las sesiones se registran como login/logout. */
    public boolean isIgnored(String entityName) {
        return AUDIT_LOG.equals(entityName) || USER_SESSION.equals(entityName);
    }

    /** Borrar una sesión equivale a cerrar sesión. */
    public boolean isSession(String entityName) {
        return USER_SESSION.equals(entityName);
    }
}