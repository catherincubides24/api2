package com.petshop.audit;

import com.petshop.entity.AuditAction;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

/** Traduce los eventos de persistencia de Hibernate en registros de auditoría. */
@Component
@RequiredArgsConstructor
public class HibernateAuditListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final String AUTH_ENTITY = "Auth";

    private final AuditLogRecorder recorder;
    private final AuditActorResolver actorResolver;
    private final AuditSnapshotBuilder snapshotBuilder;
    private final AuditPolicy policy;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        EntityPersister persister = event.getPersister();
        String entity = simpleName(persister);
        if (policy.isIgnored(entity)) {
            return;
        }
        String details = snapshotBuilder.snapshot(
                persister.getPropertyNames(), persister.getPropertyTypes(),
                event.getState(), event.getSession().getFactory());

        recorder.record(AuditAction.CREATE, entity, String.valueOf(event.getId()),
                actorResolver.resolveActor(), details);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        EntityPersister persister = event.getPersister();
        String entity = simpleName(persister);
        if (policy.isIgnored(entity)) {
            return;
        }
        snapshotBuilder.changes(
                        persister.getPropertyNames(), persister.getPropertyTypes(),
                        event.getOldState(), event.getState(), event.getSession().getFactory())
                .ifPresent(details -> recorder.record(AuditAction.UPDATE, entity,
                        String.valueOf(event.getId()), actorResolver.resolveActor(), details));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        EntityPersister persister = event.getPersister();
        String entity = simpleName(persister);

        if (policy.isSession(entity)) {
            recorder.record(AuditAction.LOGOUT, AUTH_ENTITY, null, actorResolver.resolveActor(), null);
            return;
        }
        if (policy.isIgnored(entity)) {
            return;
        }
        String details = snapshotBuilder.snapshot(
                persister.getPropertyNames(), persister.getPropertyTypes(),
                event.getDeletedState(), event.getSession().getFactory());

        recorder.record(AuditAction.DELETE, entity, String.valueOf(event.getId()),
                actorResolver.resolveActor(), details);
    }

    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private String simpleName(EntityPersister persister) {
        String fullName = persister.getEntityName();
        return fullName.substring(fullName.lastIndexOf('.') + 1);
    }
}