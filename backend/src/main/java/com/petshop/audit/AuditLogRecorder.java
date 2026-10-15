package com.petshop.audit;

import com.petshop.entity.AuditAction;
import com.petshop.entity.AuditLog;
import com.petshop.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Único punto de escritura de logs.
 * Si hay una transacción de negocio en curso, espera a que haga commit (no deja logs de cambios
 * revertidos) y guarda en una transacción independiente. Un fallo al auditar nunca rompe el negocio.
 */
@Slf4j
@Component
public class AuditLogRecorder {

    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate requiresNewTransaction;

    public AuditLogRecorder(AuditLogRepository auditLogRepository, PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(AuditAction action, String entityName, String entityId, String actor, String details) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .actor(actor)
                .details(details)
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persist(entry);
                }
            });
        } else {
            persist(entry);
        }
    }

    private void persist(AuditLog entry) {
        try {
            requiresNewTransaction.executeWithoutResult(status -> auditLogRepository.save(entry));
        } catch (RuntimeException ex) {
            log.error("No se pudo guardar el log de auditoría ({} {}): {}",
                    entry.getAction(), entry.getEntityName(), ex.getMessage());
        }
    }
}