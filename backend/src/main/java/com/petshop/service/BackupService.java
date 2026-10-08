package com.petshop.service;

import com.petshop.dto.backup.DatabaseInfoDto;
import com.petshop.dto.backup.RestoreResponse;
import com.petshop.entity.Role;
import com.petshop.entity.User;
import com.petshop.exception.BadRequestException;
import com.petshop.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final DataSource dataSource;
    private final UserRepository userRepository;

    /**
     * Orden de tablas para exportación e inserción global respetando claves foráneas.
     */
    private static final List<String> TABLES_IN_ORDER = List.of(
            "users",
            "products",
            "orders",
            "order_items",
            "user_sessions"
    );

    /**
     * Secuencias asociadas a las tablas con auto-incremento (SERIAL/IDENTITY).
     */
    private static final Map<String, String> TABLE_SEQUENCES = Map.of(
            "users", "users_id_seq",
            "products", "products_id_seq",
            "orders", "orders_id_seq",
            "order_items", "order_items_id_seq",
            "user_sessions", "user_sessions_id_seq"
    );

    /**
     * Obtiene el usuario autenticado actual.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("No autenticado");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
    }

    /**
     * Genera el nombre del archivo según el rol del usuario actual.
     */
    public String generateBackupFilename(String timestamp) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.CUSTOMER) {
            return "huellitas_compras_cliente_" + currentUser.getId() + "_" + timestamp + ".sql";
        }
        return "huellitas_shop_backup_" + timestamp + ".sql";
    }

    /**
     * Genera un script SQL. Si es CUSTOMER, exporta únicamente sus compras y pedidos.
     * Si es ADMIN o EMPLOYEE, genera la copia completa de la base de datos.
     */
    public byte[] exportDatabaseSql() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.CUSTOMER) {
            return exportCustomerBackupSql(currentUser);
        } else {
            return exportFullDatabaseSql();
        }
    }

    /**
     * Exportación de respaldo exclusiva para CLIENTE: solo sus pedidos y sus compras.
     */
    private byte[] exportCustomerBackupSql(User customer) {
        StringBuilder sql = new StringBuilder();
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sql.append("-- ======================================================================\n");
        sql.append("-- Huellitas Shop - Copia de Seguridad Personal de Compras y Pedidos\n");
        sql.append("-- Cliente: ").append(customer.getFullName()).append(" (").append(customer.getEmail()).append(")\n");
        sql.append("-- ID de Cliente: ").append(customer.getId()).append("\n");
        sql.append("-- Generado el: ").append(generatedAt).append("\n");
        sql.append("-- Motor compatible: PostgreSQL 16+\n");
        sql.append("-- NOTA: Este respaldo contiene exclusivamente las compras y pedidos de este cliente.\n");
        sql.append("-- ======================================================================\n\n");

        sql.append("SET client_encoding = 'UTF8';\n");
        sql.append("SET standard_conforming_strings = on;\n\n");
        sql.append("BEGIN;\n\n");

        // 1. Limpieza de compras previas del cliente (NO afecta a otros usuarios)
        sql.append("-- ----------------------------------------------------------------------\n");
        sql.append("-- 1. Limpieza de pedidos previos de este cliente\n");
        sql.append("-- ----------------------------------------------------------------------\n");
        sql.append("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id = ")
           .append(customer.getId()).append(");\n");
        sql.append("DELETE FROM orders WHERE user_id = ").append(customer.getId()).append(";\n\n");

        long totalExportedRows = 0;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 2. Pedidos del cliente
            sql.append("-- ----------------------------------------------------------------------\n");
            sql.append("-- 2. Pedidos exclusivos del cliente\n");
            sql.append("-- ----------------------------------------------------------------------\n");
            long orderCount = 0;
            try (ResultSet rs = statement.executeQuery("SELECT * FROM orders WHERE user_id = " + customer.getId() + " ORDER BY id ASC")) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columnNames = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(meta.getColumnName(i));
                }
                String columnsList = String.join(", ", columnNames);

                while (rs.next()) {
                    orderCount++;
                    totalExportedRows++;
                    StringBuilder rowValues = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) rowValues.append(", ");
                        rowValues.append(formatSqlValue(rs, i, meta.getColumnType(i)));
                    }
                    sql.append("INSERT INTO orders (").append(columnsList).append(") VALUES (")
                       .append(rowValues).append(");\n");
                }
            }
            sql.append("-- Fin pedidos del cliente (").append(orderCount).append(" pedidos)\n\n");

            // 3. Artículos de los pedidos del cliente (compras)
            sql.append("-- ----------------------------------------------------------------------\n");
            sql.append("-- 3. Artículos comprados por el cliente\n");
            sql.append("-- ----------------------------------------------------------------------\n");
            long itemsCount = 0;
            String itemsQuery = "SELECT oi.* FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.user_id = " + customer.getId() + " ORDER BY oi.id ASC";
            try (ResultSet rs = statement.executeQuery(itemsQuery)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columnNames = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(meta.getColumnName(i));
                }
                String columnsList = String.join(", ", columnNames);

                while (rs.next()) {
                    itemsCount++;
                    totalExportedRows++;
                    StringBuilder rowValues = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) rowValues.append(", ");
                        rowValues.append(formatSqlValue(rs, i, meta.getColumnType(i)));
                    }
                    sql.append("INSERT INTO order_items (").append(columnsList).append(") VALUES (")
                       .append(rowValues).append(");\n");
                }
            }
            sql.append("-- Fin detalles de compras (").append(itemsCount).append(" artículos)\n\n");

            // 4. Sincronización de secuencias
            sql.append("-- ----------------------------------------------------------------------\n");
            sql.append("-- 4. Sincronización de secuencias autonuméricas\n");
            sql.append("-- ----------------------------------------------------------------------\n");
            sql.append("SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 1), (SELECT COUNT(*) > 0 FROM orders));\n");
            sql.append("SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 1), (SELECT COUNT(*) > 0 FROM order_items));\n");

            sql.append("\nCOMMIT;\n\n");
            sql.append("-- ======================================================================\n");
            sql.append("-- Total registros en respaldo de compras del cliente: ").append(totalExportedRows).append("\n");
            sql.append("-- Fin del respaldo personal\n");
            sql.append("-- ======================================================================\n");

        } catch (Exception e) {
            log.error("Error al exportar la copia de seguridad personal del cliente", e);
            throw new RuntimeException("Error al generar la copia de seguridad SQL del cliente: " + e.getMessage(), e);
        }

        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Exportación completa de toda la base de datos (para Administrador y Trabajador).
     */
    private byte[] exportFullDatabaseSql() {
        StringBuilder sql = new StringBuilder();
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sql.append("-- ======================================================================\n");
        sql.append("-- Huellitas Shop - Copia de Seguridad de Base de Datos\n");
        sql.append("-- Generado el: ").append(generatedAt).append("\n");
        sql.append("-- Motor compatible: PostgreSQL 16+\n");
        sql.append("-- ======================================================================\n\n");

        sql.append("SET client_encoding = 'UTF8';\n");
        sql.append("SET standard_conforming_strings = on;\n\n");
        sql.append("BEGIN;\n\n");

        // 1. Limpieza de tablas existentes en orden inverso de dependencias
        sql.append("-- ----------------------------------------------------------------------\n");
        sql.append("-- 1. Limpieza de tablas existentes\n");
        sql.append("-- ----------------------------------------------------------------------\n");
        sql.append("TRUNCATE TABLE order_items, orders, user_sessions, products, users RESTART IDENTITY CASCADE;\n\n");

        // 2. Exportación de datos de cada tabla
        sql.append("-- ----------------------------------------------------------------------\n");
        sql.append("-- 2. Inserción de datos\n");
        sql.append("-- ----------------------------------------------------------------------\n\n");

        long totalExportedRows = 0;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            for (String tableName : TABLES_IN_ORDER) {
                sql.append("-- Tabla: ").append(tableName).append("\n");

                String query = "SELECT * FROM " + tableName + " ORDER BY id ASC";
                try (ResultSet rs = statement.executeQuery(query)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    List<String> columnNames = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(meta.getColumnName(i));
                    }
                    String columnsList = String.join(", ", columnNames);

                    long rowCount = 0;
                    while (rs.next()) {
                        rowCount++;
                        totalExportedRows++;
                        StringBuilder rowValues = new StringBuilder();

                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) {
                                rowValues.append(", ");
                            }
                            rowValues.append(formatSqlValue(rs, i, meta.getColumnType(i)));
                        }

                        sql.append("INSERT INTO ").append(tableName)
                           .append(" (").append(columnsList).append(") VALUES (")
                           .append(rowValues).append(");\n");
                    }

                    sql.append("-- Fin tabla ").append(tableName)
                       .append(" (").append(rowCount).append(" registros)\n\n");
                }
            }

            // 3. Sincronización de secuencias autonuméricas
            sql.append("-- ----------------------------------------------------------------------\n");
            sql.append("-- 3. Sincronización de secuencias de IDs\n");
            sql.append("-- ----------------------------------------------------------------------\n");
            for (Map.Entry<String, String> entry : TABLE_SEQUENCES.entrySet()) {
                String table = entry.getKey();
                String seq = entry.getValue();
                sql.append("SELECT setval('").append(seq)
                   .append("', COALESCE((SELECT MAX(id) FROM ").append(table).append("), 1), ")
                   .append("(SELECT COUNT(*) > 0 FROM ").append(table).append("));\n");
            }

            sql.append("\nCOMMIT;\n\n");
            sql.append("-- ======================================================================\n");
            sql.append("-- Total de registros exportados: ").append(totalExportedRows).append("\n");
            sql.append("-- Fin del respaldo completo\n");
            sql.append("-- ======================================================================\n");

        } catch (Exception e) {
            log.error("Error al exportar la base de datos a SQL", e);
            throw new RuntimeException("Error al generar la copia de seguridad SQL: " + e.getMessage(), e);
        }

        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Restaura la base de datos ejecutando el script SQL recibido en una transacción atómica.
     */
    public RestoreResponse restoreDatabase(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Debe seleccionar un archivo para restaurar la base de datos.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".sql")) {
            throw new BadRequestException("El archivo debe tener formato .sql (archivo SQL válido).");
        }

        String sqlContent;
        try {
            sqlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al leer el archivo SQL", e);
            throw new BadRequestException("No se pudo leer el archivo de respaldo: " + e.getMessage());
        }

        List<String> statements = parseSqlStatements(sqlContent);
        if (statements.isEmpty()) {
            throw new BadRequestException("El archivo SQL no contiene sentencias ejecutables válidas.");
        }

        User currentUser = getCurrentUser();
        boolean isCustomer = currentUser.getRole() == Role.CUSTOMER;

        if (isCustomer) {
            for (String rawStmt : statements) {
                validateCustomerStatement(rawStmt, currentUser.getId());
            }
        }

        int executedCount = 0;

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                for (String rawStmt : statements) {
                    String stmt = rawStmt.trim();
                    if (stmt.isEmpty()) {
                        continue;
                    }

                    // Ignorar BEGIN y COMMIT dentro del archivo para que la transacción sea controlada por JDBC
                    String upper = stmt.toUpperCase();
                    if (upper.equals("BEGIN") || upper.equals("BEGIN;") ||
                        upper.equals("COMMIT") || upper.equals("COMMIT;")) {
                        continue;
                    }

                    statement.execute(stmt);
                    executedCount++;
                }

                connection.commit();
                log.info("Restauración de base de datos exitosa. Sentencias ejecutadas: {}, rol: {}",
                        executedCount, currentUser.getRole());

            } catch (Exception ex) {
                connection.rollback();
                log.error("Error durante la ejecución del script SQL de restauración, transacción revertida", ex);
                throw new BadRequestException("Error al restaurar la base de datos: " + ex.getMessage());
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error al conectar con la base de datos para restauración", e);
            throw new RuntimeException("Fallo en la conexión de base de datos durante restauración: " + e.getMessage(), e);
        }

        String successMessage = isCustomer
                ? "Copia de seguridad de tus compras y pedidos restaurada exitosamente."
                : "Base de datos restaurada exitosamente.";

        return new RestoreResponse(
                true,
                successMessage,
                originalFilename,
                executedCount,
                LocalDateTime.now()
        );
    }

    /**
     * Valida que una sentencia SQL pertenezca estrictamente a pedidos y compras del cliente.
     */
    private void validateCustomerStatement(String stmt, Long customerId) {
        String clean = stmt.trim();
        String upper = clean.toUpperCase();

        if (upper.startsWith("SET ") || upper.equals("BEGIN") || upper.equals("COMMIT")) {
            return;
        }
        if (upper.startsWith("SELECT SETVAL('ORDERS_ID_SEQ'") || upper.startsWith("SELECT SETVAL('ORDER_ITEMS_ID_SEQ'")) {
            return;
        }
        if (upper.startsWith("DELETE FROM ORDER_ITEMS")) {
            if (!upper.contains("USER_ID = " + customerId) && !upper.contains("USER_ID=" + customerId)) {
                throw new BadRequestException("No tienes permiso para modificar compras de otros usuarios.");
            }
            return;
        }
        if (upper.startsWith("DELETE FROM ORDERS")) {
            if (!upper.contains("USER_ID = " + customerId) && !upper.contains("USER_ID=" + customerId)) {
                throw new BadRequestException("No tienes permiso para modificar pedidos de otros usuarios.");
            }
            return;
        }
        if (upper.startsWith("INSERT INTO ORDERS")) {
            if (!clean.contains(String.valueOf(customerId))) {
                throw new BadRequestException("No puedes restaurar pedidos que no corresponden a tu cuenta.");
            }
            return;
        }
        if (upper.startsWith("INSERT INTO ORDER_ITEMS")) {
            return;
        }

        throw new BadRequestException("Operación no permitida en respaldo de compras de cliente: " + clean);
    }

    /**
     * Obtiene estadísticas actuales de la base de datos para la pantalla de backup.
     * Si es CLIENTE, muestra métricas enfocadas únicamente en sus pedidos y compras.
     */
    public DatabaseInfoDto getDatabaseInfo() {
        User currentUser = getCurrentUser();
        boolean isCustomer = currentUser.getRole() == Role.CUSTOMER;

        Map<String, Long> tableCounts = new LinkedHashMap<>();
        long totalRecords = 0;
        String dbName = "petshop_db";
        String dbVersion = "PostgreSQL";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            DatabaseMetaData metaData = connection.getMetaData();
            dbVersion = metaData.getDatabaseProductVersion();
            if (connection.getCatalog() != null && !connection.getCatalog().isBlank()) {
                dbName = connection.getCatalog();
            }

            if (isCustomer) {
                long myOrders = 0;
                long myItems = 0;

                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM orders WHERE user_id = " + currentUser.getId())) {
                    if (rs.next()) myOrders = rs.getLong(1);
                }
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(oi.id) FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.user_id = " + currentUser.getId())) {
                    if (rs.next()) myItems = rs.getLong(1);
                }

                tableCounts.put("mis_pedidos", myOrders);
                tableCounts.put("mis_articulos_comprados", myItems);
                totalRecords = myOrders + myItems;
            } else {
                for (String tableName : TABLES_IN_ORDER) {
                    try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                        if (rs.next()) {
                            long count = rs.getLong(1);
                            tableCounts.put(tableName, count);
                            totalRecords += count;
                        } else {
                            tableCounts.put(tableName, 0L);
                        }
                    } catch (Exception ex) {
                        log.warn("No se pudo obtener conteo para tabla {}", tableName, ex);
                        tableCounts.put(tableName, 0L);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error al obtener información de la base de datos", e);
        }

        return new DatabaseInfoDto(
                dbName,
                dbVersion,
                tableCounts,
                totalRecords,
                LocalDateTime.now()
        );
    }

    /**
     * Formatea un valor de columna de base de datos a sintaxis SQL válida.
     */
    private String formatSqlValue(ResultSet rs, int columnIndex, int columnType) throws Exception {
        Object value = rs.getObject(columnIndex);
        if (value == null) {
            return "NULL";
        }

        switch (columnType) {
            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(columnIndex) ? "TRUE" : "FALSE";

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return String.valueOf(rs.getLong(columnIndex));

            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return rs.getBigDecimal(columnIndex).toPlainString();

            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "'" + rs.getString(columnIndex).replace("'", "''") + "'";

            default:
                String strValue = rs.getString(columnIndex);
                return "'" + strValue.replace("'", "''") + "'";
        }
    }

    /**
     * Parsea un script SQL en sentencias individuales respetando cadenas entre comillas
     * y comentarios simples (-- ) y de bloque (/* *&#47;).
     */
    private List<String> parseSqlStatements(String sqlScript) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        int length = sqlScript.length();

        for (int i = 0; i < length; i++) {
            char c = sqlScript.charAt(i);
            char next = (i + 1 < length) ? sqlScript.charAt(i + 1) : '\0';

            // Comentario de línea (-- )
            if (!inSingleQuote && !inBlockComment && !inLineComment && c == '-' && next == '-') {
                inLineComment = true;
                i++; // saltar el segundo '-'
                continue;
            }

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            // Comentario de bloque (/* ... */)
            if (!inSingleQuote && !inLineComment && !inBlockComment && c == '/' && next == '*') {
                inBlockComment = true;
                i++; // saltar el '*'
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++; // saltar el '/'
                }
                continue;
            }

            // Manejo de comillas simples ('...') y comillas dobles escapadas ('')
            if (c == '\'') {
                if (inSingleQuote && next == '\'') {
                    // Comilla simple escapada ''
                    currentStatement.append("''");
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                currentStatement.append(c);
                continue;
            }

            // Fin de sentencia al encontrar ';' fuera de comillas y comentarios
            if (c == ';' && !inSingleQuote) {
                String stmt = currentStatement.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                currentStatement.setLength(0);
                continue;
            }

            currentStatement.append(c);
        }

        String remaining = currentStatement.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }
}
