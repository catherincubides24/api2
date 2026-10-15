package com.petshop.audit;

import com.petshop.entity.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Registra inicios de sesión exitosos y fallidos (no son cambios de entidades). */
@Component
@RequiredArgsConstructor
public class AuthenticationAuditListener {

    private static final String ENTITY = "Auth";

    private final AuditLogRecorder recorder;

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        recorder.record(AuditAction.LOGIN_SUCCESS, ENTITY, null, event.getAuthentication().getName(), null);
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        recorder.record(AuditAction.LOGIN_FAILURE, ENTITY, null, event.getAuthentication().getName(),
                "Credenciales inválidas");
    }
}