package com.petshop.service;

import com.petshop.config.SessionPolicy;
import com.petshop.config.SessionProperties;
import com.petshop.entity.User;
import com.petshop.entity.UserSession;
import com.petshop.exception.SessionConflictException;
import com.petshop.repository.UserSessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Garantiza una única sesión activa por usuario (un solo navegador a la vez). */
@Service
@RequiredArgsConstructor
public class ActiveSessionService {

    private final UserSessionRepository sessionRepository;
    private final SessionProperties properties;

    /** Abre una sesión y devuelve su identificador (se incrusta en el JWT). */
    @Transactional
    public String open(User user) {
        String newSessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Optional<UserSession> existing = sessionRepository.findByUserEmail(user.getEmail());
        if (existing.isEmpty()) {
            sessionRepository.save(UserSession.builder()
                    .user(user)
                    .sessionId(newSessionId)
                    .lastSeenAt(now)
                    .build());
            return newSessionId;
        }

        UserSession current = existing.get();
        if (properties.policy() == SessionPolicy.BLOCK && isActive(current)) {
            throw new SessionConflictException(
                    "Ya hay una sesión abierta en otro navegador. Ciérrala o espera unos minutos e inténtalo de nuevo.");
        }

        current.setSessionId(newSessionId);
        current.setLastSeenAt(now);
        return newSessionId;
    }

    @Transactional(readOnly = true)
    public boolean isCurrent(String email, String sessionId) {
        if (sessionId == null) {
            return false;
        }
        return sessionRepository.findByUserEmail(email)
                .map(session -> session.getSessionId().equals(sessionId))
                .orElse(false);
    }

    @Transactional
    public void heartbeat(String email) {
        sessionRepository.findByUserEmail(email)
                .ifPresent(session -> session.setLastSeenAt(LocalDateTime.now()));
    }

    @Transactional
    public void close(String email) {
        sessionRepository.findByUserEmail(email).ifPresent(sessionRepository::delete);
    }

    private boolean isActive(UserSession session) {
        return session.getLastSeenAt()
                .plus(properties.inactivityTimeout())
                .isAfter(LocalDateTime.now());
    }
}