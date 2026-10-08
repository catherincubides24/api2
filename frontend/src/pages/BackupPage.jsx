import {
  AlertTriangle,
  CheckCircle2,
  Database,
  Download,
  FileCode,
  HardDrive,
  Loader2,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  UploadCloud,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { backupService } from "../services/backupService";
import { downloadBlob } from "../utils/downloadBlob";

export default function BackupPage() {
  const { user, isAdmin, isStaff, isCustomer, logout } = useAuth();
  const navigate = useNavigate();

  // Estados de información de BD
  const [dbInfo, setDbInfo] = useState(null);
  const [loadingInfo, setLoadingInfo] = useState(false);

  // Estados de descarga
  const [downloading, setDownloading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  // Estados de restauración
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [restoreResult, setRestoreResult] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");

  const fileInputRef = useRef(null);

  // Determinar etiqueta amigable del rol
  const roleLabel = isAdmin
    ? "Administrador"
    : isStaff
    ? "Trabajador (Empleado)"
    : "Cliente";

  const loadInfo = async () => {
    try {
      setLoadingInfo(true);
      setErrorMsg("");
      const data = await backupService.getDatabaseInfo();
      setDbInfo(data);
    } catch (err) {
      console.error("Error al cargar información de la base de datos", err);
    } finally {
      setLoadingInfo(false);
    }
  };

  useEffect(() => {
    loadInfo();
  }, []);

  // Manejar descarga de backup
  const handleDownload = async () => {
    try {
      setDownloading(true);
      setErrorMsg("");
      setDownloadSuccess(false);

      const response = await backupService.downloadBackup();

      // Extraer nombre del archivo del header o generar por defecto
      let fileName = "huellitas_shop_backup.sql";
      const disposition = response.headers["content-disposition"];
      if (disposition && disposition.includes("filename=")) {
        const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (match && match[1]) {
          fileName = match[1].replace(/['"]/g, "");
        }
      }

      downloadBlob(response.data, fileName);
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 5000);
    } catch (err) {
      console.error("Error al descargar la copia de seguridad", err);
      setErrorMsg(
        err.response?.data?.message ||
          "Ocurrió un error al generar la copia de seguridad."
      );
    } finally {
      setDownloading(false);
    }
  };

  // Manejar selección de archivo
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    validateAndSetFile(file);
  };

  const validateAndSetFile = (file) => {
    setErrorMsg("");
    if (!file) return;

    if (!file.name.toLowerCase().endsWith(".sql")) {
      setErrorMsg("El archivo seleccionado debe tener extensión .sql");
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  // Drag and drop
  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    validateAndSetFile(file);
  };

  // Ejecutar restauración
  const handleConfirmRestore = async () => {
    if (!selectedFile) return;

    setShowConfirmModal(false);
    setRestoring(true);
    setErrorMsg("");
    setRestoreResult(null);

    try {
      const result = await backupService.restoreBackup(selectedFile);
      setRestoreResult(result);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      // Recargar métricas
      loadInfo();
    } catch (err) {
      console.error("Error al restaurar la base de datos", err);
      setErrorMsg(
        err.response?.data?.message ||
          "Error al ejecutar la restauración. La base de datos fue revertida a su estado anterior."
      );
    } finally {
      setRestoring(false);
    }
  };

  const handleFinishAndRelogin = async () => {
    setRestoreResult(null);
    await logout();
    navigate("/login");
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(2) + " MB";
  };

  return (
    <div className="space-y-8">
      {/* Encabezado */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <SectionTitle
            title={
              isCustomer
                ? "Copia de Seguridad de Compras"
                : "Copia de Seguridad y Restauración"
            }
            subtitle={
              isCustomer
                ? "Descarga un archivo SQL con tus compras y pedidos, o restaura tu historial personal"
                : "Gestiona respaldos en formato SQL y restaura la base de datos de Huellitas Shop"
            }
          />
        </div>
        <div className="flex items-center gap-2 self-start rounded-2xl bg-white px-4 py-2.5 shadow-soft dark:bg-slate-800">
          <ShieldCheck className="text-mint" size={20} />
          <div className="text-sm">
            <span className="text-ink/60 dark:text-cream/60">Rol habilitado: </span>
            <span className="font-semibold text-ink dark:text-cream">
              {roleLabel} ({user?.fullName})
            </span>
          </div>
        </div>
      </div>

      {/* Alerta de error si existe */}
      {errorMsg && (
        <div className="flex items-center gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
          <AlertTriangle size={20} className="shrink-0 text-red-500" />
          <p className="flex-1">{errorMsg}</p>
          <button
            onClick={() => setErrorMsg("")}
            className="text-xs font-semibold underline"
          >
            Cerrar
          </button>
        </div>
      )}

      {/* Tarjeta de métricas de la base de datos */}
      <div className="rounded-3xl bg-white p-6 shadow-soft dark:bg-slate-800">
        <div className="flex items-center justify-between border-b border-ink/5 pb-4 dark:border-white/10">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-ink/5 text-ink dark:bg-white/10 dark:text-cream">
              <Database size={20} />
            </span>
            <div>
              <h3 className="font-display text-lg font-bold text-ink dark:text-cream">
                {isCustomer ? "Resumen de Compras del Cliente" : "Estado de la Base de Datos"}
              </h3>
              <p className="text-xs text-ink/60 dark:text-cream/60">
                {isCustomer
                  ? `Historial personal de ${user?.fullName || "Cliente"}`
                  : `${dbInfo?.databaseName || "petshop_db"} • ${dbInfo?.databaseVersion || "PostgreSQL 16"}`}
              </p>
            </div>
          </div>
          <button
            onClick={loadInfo}
            disabled={loadingInfo}
            title="Actualizar métricas"
            className="rounded-full p-2 text-ink/60 transition hover:bg-ink/5 hover:text-ink disabled:opacity-50 dark:text-cream/60 dark:hover:bg-white/10 dark:hover:text-cream"
          >
            <RefreshCw size={18} className={loadingInfo ? "animate-spin" : ""} />
          </button>
        </div>

        {isCustomer ? (
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Total Registros Personales
              </p>
              <p className="mt-1 text-2xl font-extrabold text-coral">
                {dbInfo?.totalRecords ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Mis Pedidos Realizados
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.mis_pedidos ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Mis Artículos Comprados
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.mis_articulos_comprados ?? 0}
              </p>
            </div>
          </div>
        ) : (
          <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Total Registros
              </p>
              <p className="mt-1 text-2xl font-extrabold text-coral">
                {dbInfo?.totalRecords ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Usuarios
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.users ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Productos
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.products ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Pedidos
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.orders ?? 0}
              </p>
            </div>
            <div className="rounded-2xl bg-sand/30 p-4 dark:bg-slate-900/60">
              <p className="text-xs font-medium uppercase tracking-wider text-ink/50 dark:text-cream/50">
                Detalles Pedido
              </p>
              <p className="mt-1 text-2xl font-extrabold text-ink dark:text-cream">
                {dbInfo?.tableCounts?.order_items ?? 0}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Grid de las 2 acciones principales */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        {/* Card 1: Descargar Copia de Seguridad */}
        <div className="flex flex-col justify-between rounded-3xl bg-white p-6 shadow-soft dark:bg-slate-800">
          <div>
            <div className="flex items-center gap-3">
              <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-coral/10 text-coral">
                <Download size={24} />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ink dark:text-cream">
                  {isCustomer ? "Respaldar Mis Compras" : "Generar Copia de Seguridad"}
                </h3>
                <p className="text-xs text-ink/60 dark:text-cream/60">
                  {isCustomer
                    ? "Exportación exclusiva de tus compras y pedidos en formato .sql"
                    : "Exportación en archivo estructurado con formato .sql"}
                </p>
              </div>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-ink/75 dark:text-cream/75">
              {isCustomer
                ? "Genera un archivo SQL descargable con el registro de todos tus pedidos y artículos comprados. El respaldo contiene exclusivamente tus transacciones personales, garantizando total privacidad."
                : "Genera un volcado completo de todas las tablas, registros y secuencias del sistema. El archivo resultante es un script SQL estándar compatible con PostgreSQL, apto para restauración directa o auditoría."}
            </p>

            <div className="mt-6 space-y-2 rounded-2xl bg-sand/30 p-4 text-xs text-ink/70 dark:bg-slate-900/60 dark:text-cream/70">
              <div className="flex items-center gap-2">
                <FileCode size={16} className="text-coral" />
                <span>Extensión de salida: <strong className="font-semibold text-ink dark:text-cream">.sql</strong></span>
              </div>
              <div className="flex items-center gap-2">
                <HardDrive size={16} className="text-coral" />
                <span>
                  {isCustomer
                    ? "Incluye: Exclusivamente tus pedidos y artículos comprados."
                    : "Incluye: Tablas, datos, limpieza previa e índices/secuencias."}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={16} className="text-mint" />
                <span>
                  {isCustomer
                    ? "Privacidad: Aislado, sin acceso a compras de otros clientes."
                    : "Codificación: UTF-8 con sentencias transaccionales atómicas."}
                </span>
              </div>
            </div>
          </div>

          <div className="mt-8 pt-4">
            {downloadSuccess && (
              <div className="mb-4 flex items-center gap-2 text-sm font-semibold text-emerald-600 dark:text-emerald-400">
                <CheckCircle2 size={18} />
                <span>
                  {isCustomer
                    ? "¡Copia de compras descargada exitosamente!"
                    : "¡Copia de seguridad descargada exitosamente!"}
                </span>
              </div>
            )}
            <button
              onClick={handleDownload}
              disabled={downloading}
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-coral px-6 py-3.5 font-semibold text-white shadow-soft transition hover:bg-coral/90 disabled:opacity-50"
            >
              {downloading ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Generando archivo SQL...</span>
                </>
              ) : (
                <>
                  <Download size={18} />
                  <span>
                    {isCustomer
                      ? "Descargar Mis Compras (.sql)"
                      : "Descargar Copia de Seguridad (.sql)"}
                  </span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Card 2: Restaurar Copia de Seguridad */}
        <div className="flex flex-col justify-between rounded-3xl bg-white p-6 shadow-soft dark:bg-slate-800">
          <div>
            <div className="flex items-center gap-3">
              <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-mint/20 text-emerald-700 dark:text-mint">
                <RotateCcw size={24} />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ink dark:text-cream">
                  {isCustomer ? "Restaurar Mis Pedidos" : "Restaurar Copia de Seguridad"}
                </h3>
                <p className="text-xs text-ink/60 dark:text-cream/60">
                  {isCustomer
                    ? "Restaura tu historial de compras desde un archivo .sql"
                    : "Importa y restablece los datos a partir de un archivo .sql"}
                </p>
              </div>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-ink/75 dark:text-cream/75">
              {isCustomer
                ? "Carga un archivo de respaldo con extensión .sql para restaurar tu historial de compras en tu cuenta de Huellitas Shop."
                : "Carga un archivo de respaldo con extensión .sql para restaurar el estado de la base de datos. La operación se ejecuta en una transacción protegida contra fallos."}
            </p>

            {/* Zona de Drag & Drop */}
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`mt-4 flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed p-6 text-center transition ${
                isDragging
                  ? "border-coral bg-coral/5"
                  : selectedFile
                  ? "border-emerald-500 bg-emerald-50/50 dark:bg-emerald-950/20"
                  : "border-ink/15 hover:border-ink/30 dark:border-white/15 dark:hover:border-white/30"
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".sql"
                onChange={handleFileChange}
                className="hidden"
              />

              {selectedFile ? (
                <div className="flex flex-col items-center gap-2">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600 dark:bg-emerald-900/50 dark:text-emerald-400">
                    <FileCode size={22} />
                  </span>
                  <p className="font-semibold text-ink dark:text-cream">
                    {selectedFile.name}
                  </p>
                  <p className="text-xs text-ink/60 dark:text-cream/60">
                    Tamaño: {formatFileSize(selectedFile.size)} &bull; Haz clic para cambiar archivo
                  </p>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2">
                  <UploadCloud size={32} className="text-ink/40 dark:text-cream/40" />
                  <p className="text-sm font-semibold text-ink dark:text-cream">
                    Arrastra tu archivo .sql aquí o haz clic para seleccionarlo
                  </p>
                  <p className="text-xs text-ink/50 dark:text-cream/50">
                    Únicamente archivos con extensión .sql (máx. 50 MB)
                  </p>
                </div>
              )}
            </div>

            {/* Advertencia de restauración */}
            <div className="mt-4 flex items-start gap-2.5 rounded-2xl bg-amber-50 p-3.5 text-xs text-amber-900 dark:bg-amber-950/30 dark:text-amber-200">
              <AlertTriangle size={18} className="shrink-0 text-amber-600 dark:text-amber-400" />
              <p>
                <strong>Atención:</strong>{" "}
                {isCustomer
                  ? "La restauración actualizará tus pedidos con los datos del archivo SQL. Los registros de otros clientes no se verán afectados."
                  : "La restauración reemplazará los datos actuales de la base de datos con los registros del archivo SQL. Esta acción no se puede deshacer."}
              </p>
            </div>
          </div>

          <div className="mt-6 pt-4">
            <button
              onClick={() => setShowConfirmModal(true)}
              disabled={!selectedFile || restoring}
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-6 py-3.5 font-semibold text-cream shadow-soft transition hover:bg-dusk disabled:opacity-40 dark:bg-white dark:text-ink dark:hover:bg-cream"
            >
              {restoring ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Restaurando...</span>
                </>
              ) : (
                <>
                  <RotateCcw size={18} />
                  <span>
                    {isCustomer ? "Restaurar Mis Compras" : "Restaurar Base de Datos"}
                  </span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Modal de confirmación previa a la restauración */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-soft dark:bg-slate-800">
            <div className="flex items-center gap-3 text-amber-600 dark:text-amber-400">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-100 dark:bg-amber-950/50">
                <AlertTriangle size={24} />
              </span>
              <h3 className="font-display text-xl font-bold text-ink dark:text-cream">
                ¿Confirmar Restauración?
              </h3>
            </div>

            <p className="mt-4 text-sm text-ink/75 dark:text-cream/75">
              {isCustomer
                ? "Estás a punto de restaurar tus pedidos con el archivo:"
                : "Estás a punto de restaurar la base de datos con el archivo:"}
            </p>
            <div className="mt-2 rounded-xl bg-sand/40 p-3 text-xs font-semibold text-ink dark:bg-slate-900/60 dark:text-cream">
              📄 {selectedFile?.name} ({formatFileSize(selectedFile?.size || 0)})
            </div>
            <p className="mt-3 text-xs text-ink/60 dark:text-cream/60">
              {isCustomer
                ? "Tus pedidos y compras serán restablecidos con los datos de esta copia personal."
                : "Los registros actuales serán limpiados e insertados con los datos de esta copia."}
            </p>

            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setShowConfirmModal(false)}
                className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink transition hover:bg-sand/30 dark:border-white/20 dark:text-cream dark:hover:bg-white/10"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmRestore}
                className="rounded-full bg-red-600 px-5 py-2 text-sm font-semibold text-white shadow-soft transition hover:bg-red-700"
              >
                Sí, restaurar ahora
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Éxito de Restauración */}
      {restoreResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-soft dark:bg-slate-800">
            <div className="flex items-center gap-3 text-emerald-600 dark:text-emerald-400">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-100 dark:bg-emerald-950/50">
                <CheckCircle2 size={24} />
              </span>
              <h3 className="font-display text-xl font-bold text-ink dark:text-cream">
                ¡Restauración Exitosa!
              </h3>
            </div>

            <p className="mt-4 text-sm text-ink/80 dark:text-cream/80">
              {restoreResult.message}
            </p>

            <div className="mt-4 space-y-1.5 rounded-2xl bg-sand/30 p-4 text-xs text-ink/70 dark:bg-slate-900/60 dark:text-cream/70">
              <p>
                <strong>Archivo aplicado:</strong> {restoreResult.fileName}
              </p>
              <p>
                <strong>Sentencias ejecutadas:</strong> {restoreResult.statementsExecuted}
              </p>
              <p>
                <strong>Fecha/Hora:</strong>{" "}
                {new Date(restoreResult.restoredAt).toLocaleString("es-CO")}
              </p>
            </div>

            <p className="mt-4 text-xs text-ink/60 dark:text-cream/60">
              Para sincronizar las sesiones activas y asegurar que todos los datos
              se visualicen actualizados, se cerrará tu sesión actual.
            </p>

            <div className="mt-6 flex justify-end">
              <button
                onClick={handleFinishAndRelogin}
                className="rounded-full bg-ink px-6 py-2.5 text-sm font-semibold text-cream shadow-soft transition hover:bg-dusk dark:bg-white dark:text-ink dark:hover:bg-cream"
              >
                Aceptar e Iniciar Sesión
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
