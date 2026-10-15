import { FileSpreadsheet, FileText } from "lucide-react";
import { useEffect, useState } from "react";
import SectionTitle from "../components/SectionTitle";
import { useLanguage } from "../context/LanguageContext";
import { productService } from "../services/productService";
import { reportService } from "../services/reportService";
import { downloadBlob } from "../utils/downloadBlob";
import { formatCurrency } from "../utils/formatCurrency";

const emptyFilters = { from: "", to: "", status: "", paymentMethod: "", category: "" };
const fieldClass =
  "rounded-xl border border-ink/15 px-3 py-2 text-sm outline-none ring-coral/30 focus:ring";

function Kpi({ label, value }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-soft">
      <p className="text-xs uppercase tracking-wide text-ink/50">{label}</p>
      <p className="mt-1 text-2xl font-extrabold text-coral">{value}</p>
    </div>
  );
}

function PercentBars({ title, items, emptyText }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-soft">
      <h3 className="mb-3 font-semibold text-ink">{title}</h3>
      {items.length === 0 && <p className="text-sm text-ink/50">{emptyText}</p>}
      {items.map((item) => (
        <div key={item.label} className="mb-2">
          <div className="flex justify-between text-xs text-ink/70">
            <span>{item.label}</span>
            <span>{item.percentage.toFixed(1)}%</span>
          </div>
          <div className="h-2 rounded-full bg-sand">
            <div
              className="h-2 rounded-full bg-mint"
              style={{ width: `${item.percentage}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

export default function AdminReportsPage() {
  const { t, tCategory } = useLanguage();
  const [filters, setFilters] = useState(emptyFilters);
  const [categories, setCategories] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState("");
  const [error, setError] = useState("");

  const setField = (name) => (event) =>
    setFilters((current) => ({ ...current, [name]: event.target.value }));

  const loadSummary = async (activeFilters = filters) => {
    try {
      setLoading(true);
      setError("");
      setSummary(await reportService.getSummary(activeFilters));
    } catch (err) {
      setError(err.response?.data?.message || t("adminReports.loadError"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSummary(emptyFilters);
    productService
      .getAdminProducts()
      .then((products) =>
        setCategories([...new Set(products.map((p) => p.category).filter(Boolean))])
      )
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDownload = async (format) => {
    try {
      setDownloading(format);
      setError("");
      const blob = await reportService.download(format, filters);
      downloadBlob(blob, `reporte-huellitas.${format.toLowerCase()}`);
    } catch {
      setError(t("adminReports.downloadError"));
    } finally {
      setDownloading("");
    }
  };

  const clearFilters = () => {
    setFilters(emptyFilters);
    loadSummary(emptyFilters);
  };

  return (
    <div className="space-y-6">
      <section className="space-y-4 rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow={t("adminReports.eyebrow")}
          title={t("adminReports.title")}
          description={t("adminReports.description")}
        />

        <div className="grid gap-3 md:grid-cols-5">
          <input type="date" value={filters.from} onChange={setField("from")} className={fieldClass} />
          <input type="date" value={filters.to} onChange={setField("to")} className={fieldClass} />
          <select value={filters.status} onChange={setField("status")} className={fieldClass}>
            <option value="">{t("adminReports.filters.allStatuses")}</option>
            <option value="PENDING">{t("orderStatus.PENDING")}</option>
            <option value="PAID">{t("orderStatus.PAID")}</option>
            <option value="SHIPPED">{t("orderStatus.SHIPPED")}</option>
            <option value="CANCELLED">{t("orderStatus.CANCELLED")}</option>
          </select>
          <select value={filters.paymentMethod} onChange={setField("paymentMethod")} className={fieldClass}>
            <option value="">{t("adminReports.filters.allPayments")}</option>
            <option value="CASH">{t("paymentMethods.CASH")}</option>
            <option value="TRANSFER">{t("paymentMethods.TRANSFER")}</option>
            <option value="PAYPAL">{t("paymentMethods.PAYPAL")}</option>
          </select>
          <select value={filters.category} onChange={setField("category")} className={fieldClass}>
            <option value="">{t("adminReports.filters.allCategories")}</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {tCategory(category)}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => loadSummary()}
            disabled={loading}
            className="rounded-xl bg-ink px-4 py-2 text-sm font-semibold text-cream disabled:opacity-60"
          >
            {loading ? t("adminReports.filters.calculating") : t("adminReports.filters.apply")}
          </button>
          <button
            onClick={clearFilters}
            className="rounded-xl border border-ink/20 px-4 py-2 text-sm font-semibold text-ink"
          >
            {t("adminReports.filters.clear")}
          </button>
          <button
            onClick={() => handleDownload("PDF")}
            disabled={Boolean(downloading)}
            className="inline-flex items-center gap-2 rounded-xl bg-coral px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            <FileText size={16} /> {downloading === "PDF" ? t("adminReports.filters.generating") : t("adminReports.filters.downloadPdf")}
          </button>
          <button
            onClick={() => handleDownload("XLSX")}
            disabled={Boolean(downloading)}
            className="inline-flex items-center gap-2 rounded-xl bg-mint px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            <FileSpreadsheet size={16} /> {downloading === "XLSX" ? t("adminReports.filters.generating") : t("adminReports.filters.downloadExcel")}
          </button>
        </div>

        {error && <p className="rounded-xl bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}
      </section>

      {summary && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Kpi label={t("adminReports.kpis.revenue")} value={formatCurrency(summary.totalRevenue)} />
            <Kpi label={t("adminReports.kpis.ordersSold")} value={`${summary.soldOrders} / ${summary.totalOrders}`} />
            <Kpi label={t("adminReports.kpis.avgTicket")} value={formatCurrency(summary.averageTicket)} />
            <Kpi label={t("adminReports.kpis.unitsSold")} value={summary.unitsSold} />
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <PercentBars title={t("adminReports.charts.ordersByStatus")} items={summary.byStatus} emptyText={t("adminReports.charts.noData")} />
            <PercentBars title={t("adminReports.charts.revenueByPayment")} items={summary.byPaymentMethod} emptyText={t("adminReports.charts.noData")} />
            <PercentBars title={t("adminReports.charts.revenueByCategory")} items={summary.byCategory} emptyText={t("adminReports.charts.noData")} />
          </div>
        </>
      )}
    </div>
  );
}