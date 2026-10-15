import { useEffect, useState } from "react";
import LogDetails from "../components/LogDetails";
import Pagination from "../components/Pagination";
import SectionTitle from "../components/SectionTitle";
import { useLanguage } from "../context/LanguageContext";
import { logService } from "../services/logService";
import { actionLabel, actionStyles, describeLog } from "../utils/logLabels";

export default function AdminLogsPage() {
  const { t, formatDate } = useLanguage();
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
      setError(err.response?.data?.message || t("adminLogs.loadError"));
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
          eyebrow={t("adminLogs.eyebrow")}
          title={t("adminLogs.title")}
          description={t("adminLogs.description")}
        />
      </section>

      {error && (
        <div className="rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm text-coral">
          {error}
        </div>
      )}

      <section className="overflow-x-auto rounded-3xl bg-white/85 p-4 shadow-card">
        {loading ? (
          <p className="p-4 text-sm text-ink/60">{t("adminLogs.loading")}</p>
        ) : pageData.content.length === 0 ? (
          <p className="p-4 text-sm text-ink/60">{t("adminLogs.empty")}</p>
        ) : (
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink/10 text-xs uppercase tracking-wide text-ink/50">
                <th className="px-3 py-2">{t("adminLogs.table.date")}</th>
                <th className="px-3 py-2">{t("adminLogs.table.action")}</th>
                <th className="px-3 py-2">{t("adminLogs.table.whatHappened")}</th>
                <th className="px-3 py-2">{t("adminLogs.table.user")}</th>
                <th className="px-3 py-2">{t("adminLogs.table.detail")}</th>
              </tr>
            </thead>
            <tbody>
              {pageData.content.map((log) => (
                <tr key={log.id} className="border-b border-ink/5 align-top last:border-0">
                  <td className="whitespace-nowrap px-3 py-2 text-ink/80">
                    {formatDate(log.createdAt, { dateStyle: "medium", timeStyle: "medium" })}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className={[
                        "inline-flex whitespace-nowrap rounded-full px-2 py-1 text-xs font-semibold",
                        actionStyles[log.action] || "bg-slate-100 text-slate-700",
                      ].join(" ")}
                    >
                      {actionLabel(log.action, t)}
                    </span>
                  </td>
                  <td className="px-3 py-2 font-semibold text-ink">{describeLog(log, t)}</td>
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