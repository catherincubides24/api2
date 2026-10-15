package com.petshop.audit;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/** Determina quién ejecuta la acción que se está auditando. */
@Component
public class AuditActorResolver {

    private static final String SYSTEM = "SYSTEM";
    private static final String ANONYMOUS = "ANONYMOUS";

    public String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }

        // Sin petición HTTP (p. ej. DataSeeder) es el sistema; con petición sin login (registro) es anónimo.
        return RequestContextHolder.getRequestAttributes() == null ? SYSTEM : ANONYMOUS;
    }
}