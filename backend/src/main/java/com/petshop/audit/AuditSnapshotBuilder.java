package com.petshop.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

/**
 * Convierte el estado de una entidad a JSON legible.
 * - Nunca incluye valores de campos sensibles (contraseña, tokens).
 * - Ignora colecciones (evita cargas lazy) y representa relaciones por id y nombre.
 */
@Component
@RequiredArgsConstructor
public class AuditSnapshotBuilder {

    private static final String MASK = "***";
    private static final String CHANGED = "[MODIFICADO]";
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "token", "refreshToken", "tokenHash");
    private static final Set<String> IGNORED_ON_UPDATE = Set.of("updatedAt", "lastSeenAt");
    /** Campos que el sistema asigna justo después de crear el registro; no son un cambio real. */
    private static final Set<String> ASSIGNED_AFTER_CREATE = Set.of("ticketNumber");
    private static final List<String> LABEL_PROPERTIES = List.of("name", "fullName", "ticketNumber");

    private final ObjectMapper objectMapper;

    /** Foto completa del estado (para CREATE y DELETE). */
    public String snapshot(String[] names, Type[] types, Object[] state, SessionFactoryImplementor factory) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            if (types[i].isCollectionType()) {
                continue;
            }
            values.put(names[i], SENSITIVE_FIELDS.contains(names[i])
                    ? MASK
                    : toLoggable(types[i], state[i], factory));
        }
        return toJson(values);
    }

    /** Solo los campos que cambiaron. Vacío si no hubo cambios relevantes. */
    public Optional<String> changes(
            String[] names, Type[] types, Object[] oldState, Object[] newState, SessionFactoryImplementor factory
    ) {
        if (oldState == null) {
            return Optional.of(snapshot(names, types, newState, factory));
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            if (types[i].isCollectionType() || IGNORED_ON_UPDATE.contains(names[i])) {
                continue;
            }
            if (unchanged(types[i], oldState[i], newState[i], factory)) {
                continue;
            }

            Object before = toLoggable(types[i], oldState[i], factory);
            Object after = toLoggable(types[i], newState[i], factory);
            if (ASSIGNED_AFTER_CREATE.contains(names[i]) && before == null) {
                continue;
            }

            if (SENSITIVE_FIELDS.contains(names[i])) {
                changes.put(names[i], CHANGED);
            } else {
                Map<String, Object> diff = new LinkedHashMap<>();
                diff.put("antes", before);
                diff.put("despues", after);
                changes.put(names[i], diff);
            }
        }

        return changes.isEmpty() ? Optional.empty() : Optional.of(toJson(changes));
    }

    /** Las relaciones se comparan por id, para no generar cambios falsos por proxies lazy. */
    private boolean unchanged(Type type, Object before, Object after, SessionFactoryImplementor factory) {
        if (type.isEntityType()) {
            return Objects.equals(identifierOf(before, factory), identifierOf(after, factory));
        }
        return Objects.equals(toLoggable(type, before, factory), toLoggable(type, after, factory));
    }

    private Object identifierOf(Object entity, SessionFactoryImplementor factory) {
        return entity == null ? null : factory.getPersistenceUnitUtil().getIdentifier(entity);
    }

    private Object toLoggable(Type type, Object value, SessionFactoryImplementor factory) {
        if (value == null) {
            return null;
        }
        if (type.isEntityType()) {
            Object id = identifierOf(value, factory);
            if (id == null) {
                return null;
            }
            return labelOf(value, factory).map(label -> id + " (" + label + ")").orElse(id.toString());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return value.toString();
    }

    /** Nombre legible de una entidad relacionada. Solo si ya está cargada (nunca fuerza consultas). */
    private Optional<String> labelOf(Object entity, SessionFactoryImplementor factory) {
        if (!Hibernate.isInitialized(entity)) {
            return Optional.empty();
        }
        Object real = Hibernate.unproxy(entity);
        EntityPersister persister = factory.getMappingMetamodel().getEntityDescriptor(real.getClass());
        List<String> properties = List.of(persister.getPropertyNames());

        for (String property : LABEL_PROPERTIES) {
            if (properties.contains(property)) {
                Object label = persister.getPropertyValue(real, property);
                if (label != null) {
                    return Optional.of(label.toString());
                }
            }
        }
        return Optional.empty();
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"no se pudo serializar el detalle\"}";
        }
    }
}