import { useEffect, useState } from "react";
import LogDetails from "../components/LogDetails";
import Pagination from "../components/Pagination";
import SectionTitle from "../components/SectionTitle";
import { logService } from "../services/logService";
import { actionLabels, actionStyles, describeLog } from "../utils/logLabels";

function formatDateTime(value) {
  return new Date(value).toLocaleString("es-CO", {
    dateStyle: "medium",
    timeStyle: "medium",
    timeZone: "America/Bogota",
  });
}

/** Historial de auditoría: solo lectura, visible únicamente para el personal (admin y trabajador). */
export default function AdminLogsPage() {
  const [pageData, setPageData] = useState({ content: [], page: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const pageSize = 15;

  const loadLogs = async (page = 0) => {
    try {
      setLoading(true);
      setError("");
      setPageData(await logService.getLogs(page, pageSize));
    } catch (err) {
      setError(err.response?.data?.message || "No se pudieron cargar los logs.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs(0);
  }, []);

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Administración"
          title="Logs del sistema"
          description="Inicios y cierres de sesión, y todo lo que se crea, modifica o elimina."
        />
      </section>

      {error && (
        <div className="rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm text-coral">
          {error}
        </div>
      )}

      <section className="overflow-x-auto rounded-3xl bg-white/85 p-4 shadow-card">
        {loading ? (
          <p className="p-4 text-sm text-ink/60">Cargando logs...</p>
        ) : pageData.content.length === 0 ? (
          <p className="p-4 text-sm text-ink/60">Aún no hay registros.</p>
        ) : (
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink/10 text-xs uppercase tracking-wide text-ink/50">
                <th className="px-3 py-2">Fecha</th>
                <th className="px-3 py-2">Acción</th>
                <th className="px-3 py-2">Qué pasó</th>
                <th className="px-3 py-2">Usuario</th>
                <th className="px-3 py-2">Detalle</th>
              </tr>
            </thead>
            <tbody>
              {pageData.content.map((log) => (
                <tr key={log.id} className="border-b border-ink/5 align-top last:border-0">
                  <td className="whitespace-nowrap px-3 py-2 text-ink/80">
                    {formatDateTime(log.createdAt)}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className={[
                        "inline-flex whitespace-nowrap rounded-full px-2 py-1 text-xs font-semibold",
                        actionStyles[log.action] || "bg-slate-100 text-slate-700",
                      ].join(" ")}
                    >
                      {actionLabels[log.action] || log.action}
                    </span>
                  </td>
                  <td className="px-3 py-2 font-semibold text-ink">{describeLog(log)}</td>
                  <td className="px-3 py-2 text-ink/80">{log.actor}</td>
                  <td className="px-3 py-2">
                    <LogDetails details={log.details} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          onPageChange={(page) => loadLogs(page)}
        />
      </section>
    </div>
  );
}