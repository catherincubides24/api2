# Contexto y Guía del Proyecto: Huellitas Shop

Este archivo proporciona el contexto persistente de la arquitectura, reglas de negocio y diseño técnico para que cualquier sesión de asistencia conozca el proyecto a fondo.

---

## 1. Descripción General
- **Nombre:** Huellitas Shop
- **Propósito:** Plataforma E-commerce Full Stack para tienda de mascotas (productos, pedidos, pagos, analítica y gestión administrativa).
- **Zona horaria oficial:** `America/Bogota` (configurada en JVM, Hibernate, serialización Jackson y visualización en frontend).
- **Moneda:** Pesos colombianos (COP).

---

## 2. Stack Tecnológico

### Backend
- **Lenguaje y Versión:** Java 17.
- **Framework:** Spring Boot 3.3.5 (Maven).
- **Seguridad:** Spring Security 6 (Stateless) + JJWT 0.12.6 + BCrypt + Google API Client (OAuth2).
- **Persistencia:** Spring Data JPA + Hibernate + PostgreSQL Driver.
- **Documentación API:** SpringDoc OpenAPI 2.6.0 (`/swagger-ui.html`, `/v3/api-docs`).
- **Integraciones:**
  - PayPal Checkout SDK (1.0.5) para pasarela de pagos.
  - Apache POI (5.3.0) para reportes en Excel (.xlsx).
  - OpenPDF (1.3.43) para reportes en PDF (.pdf).

### Frontend
- **Framework:** React 18.3 + Vite 5.4.
- **Estilos:** Tailwind CSS 3.4 (con soporte para modo oscuro/claro).
- **Enrutamiento:** React Router DOM v6.
- **PWA:** `vite-plugin-pwa` (soporte de Progressive Web App, Service Worker y Web Manifest).
- **Autenticación:** `@react-oauth/google` + JWT en `localStorage`.
- **Cliente HTTP:** Axios con interceptores para inyección de token y detección de invalidación de sesión.

### Base de Datos e Infraestructura
- **Base de Datos:** PostgreSQL 16 Alpine.
- **Gestión DB:** Adminer en puerto 8082.
- **Servidor Web:** Nginx 1.27 Alpine en frontend container (sirve estáticos y actúa como reverse proxy de `/api/` hacia el backend).
- **Orquestación:** Docker Compose.

---

## 3. Arquitectura del Código

### Backend (`/backend/src/main/java/com/petshop`)
Estructura en capas (Clean Architecture / MVC REST):
- **`config/`**:
  - `SecurityConfig`: Configuración CORS, filtros JWT y permisos por endpoint y rol.
  - `DataSeeder`: Inicializa usuario admin (`admin@petshop.com` / `Admin123!`) y productos base.
  - `PayPalConfig`: Cliente REST para llamadas a la API de PayPal.
  - `SessionProperties`, `SessionPolicy`: Configuración de inactividad y políticas de concurrencia.
- **`controller/`**:
  - `AuthController`: Registro, login, Google login, logout, heartbeat.
  - `ProductController`: Catálogo público y CRUD administrativo.
  - `OrderController`: Creación de órdenes, consulta por usuario/id, cambio de estado, generación de tickets.
  - `PayPalController`: Endpoints de creación y captura de pagos.
  - `ReportController`: Resumen de ventas y descargas en XLSX/PDF.
  - `UserController`: Administración de usuarios por administradores.
- **`service/`**:
  - `ActiveSessionService`: Garantiza una única sesión activa por usuario en base a `UserSession` y emite `SESSION_REPLACED` si se detecta concurrencia.
  - `OrderService`: Lógica transaccional de pedidos, validación de stock y control de acceso (customer vs admin).
  - `PayPalService`: Comunicación con PayPal para órdenes y captura.
  - `ReportService`, `ReportAggregator`, `OrderReportSpecifications`: Métricas y especificaciones dinámicas.
  - `report/export/`: `ExcelReportExporter` y `PdfReportExporter`.
- **`entity/`**:
  - `User` (Roles: `ADMIN`, `EMPLOYEE`, `CUSTOMER`).
  - `Product` (Categorías, precio en COP, stock, activo).
  - `PetOrder` (Estados: `PENDING`, `PAID`, `SHIPPED`, `CANCELLED`; métodos de pago: `CASH`, `PAYPAL`).
  - `OrderItem` (Relación producto-pedido con cantidad y precio unitario).
  - `UserSession` (UUID de sesión y `lastSeenAt` para control de inactividad).
- **`security/`**:
  - `JwtAuthenticationFilter`: Valida token y sesión activa en cada petición.
  - `JwtService`: Creación y validación de tokens con reclamo de sesión.

### Frontend (`/frontend/src`)
- **`context/`**:
  - `AuthContext`: Estado de autenticación, almacenamiento del token y detección de cierre por `SESSION_REPLACED`.
  - `CartContext`: Carrito de compras con persistencia local.
  - `ThemeContext`: Modo claro y oscuro.
- **`hooks/` y `components/` de Seguridad**:
  - `useIdleTimer` + `InactivityGuard`: Cierre automático tras periodo de inactividad.
  - `useSessionHeartbeat`: Pings periódicos a `/api/auth/heartbeat`.
  - `ProtectedRoute`: Control de acceso por roles (`requireAdmin`, `requireStaff`).
- **`pages/`**:
  - `HomePage`, `LoginPage`, `RegisterPage`, `CartPage`, `PurchasesPage`.
  - `PaymentSuccessPage`, `PaymentCancelPage`.
  - `AdminPage`, `AdminOrdersPage`, `AdminSalesPage`, `AdminReportsPage`.
- **`services/`**:
  - `api.js`: Instancia centralizada de Axios con interceptores.
  - Servicios especializados: `authService`, `productService`, `orderService`, `paymentService`, `reportService`, `userService`.

---

## 4. Puertos y Servicios en Local / Docker

| Servicio | Contenedor | Puerto Externo | Puerto Interno |
| :--- | :--- | :--- | :--- |
| **Frontend + Proxy** | `petshop-frontend` | 5173 | 80 |
| **Backend API** | `petshop-backend` | 8081 | 8080 |
| **PostgreSQL** | `petshop-db` | 5433 | 5432 |
| **Adminer** | `petshop-adminer` | 8082 | 8080 |

### Comandos de Utilidad
- Iniciar todo con Docker: `docker compose up --build -d`
- Reiniciar base de datos y contenedores: `docker compose down -v && docker compose up --build -d`
- Backend independiente: `mvn clean spring-boot:run` (en `/backend`)
- Frontend independiente: `npm run dev` (en `/frontend`)
