package com.petshop.controller;

import com.petshop.dto.backup.DatabaseInfoDto;
import com.petshop.dto.backup.RestoreResponse;
import com.petshop.service.BackupService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/info")
    public ResponseEntity<DatabaseInfoDto> getDatabaseInfo() {
        return ResponseEntity.ok(backupService.getDatabaseInfo());
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadBackup() {
        byte[] sqlBytes = backupService.exportDatabaseSql();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = backupService.generateBackupFilename(timestamp);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/sql"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(sqlBytes);
    }

    @PostMapping("/restore")
    public ResponseEntity<RestoreResponse> restoreBackup(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(backupService.restoreDatabase(file));
    }
}
