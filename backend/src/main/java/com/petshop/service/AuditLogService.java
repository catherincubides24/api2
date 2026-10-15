package com.petshop.service;

import com.petshop.dto.common.PageResponse;
import com.petshop.dto.log.AuditLogResponse;
import com.petshop.entity.AuditLog;
import com.petshop.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Solo consulta. La escritura vive en {@code audit.AuditLogRecorder}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public PageResponse<AuditLogResponse> getLogs(int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        Page<AuditLog> result = auditLogRepository.findAll(pageable);
        List<AuditLogResponse> content = result.getContent().stream().map(this::toResponse).toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getActor(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}