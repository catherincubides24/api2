package com.petshop.audit;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.context.annotation.Configuration;

/** Registra el listener de auditoría en Hibernate al iniciar la aplicación. */
@Configuration
@RequiredArgsConstructor
public class AuditListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final HibernateAuditListener auditListener;

    @PostConstruct
    void registerListeners() {
        EventListenerRegistry registry = entityManagerFactory
                .unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);

        registry.appendListeners(EventType.POST_INSERT, auditListener);
        registry.appendListeners(EventType.POST_UPDATE, auditListener);
        registry.appendListeners(EventType.POST_DELETE, auditListener);
    }
}