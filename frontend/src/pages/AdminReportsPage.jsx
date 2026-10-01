import { FileSpreadsheet, FileText } from "lucide-react";
import { useEffect, useState } from "react";
import SectionTitle from "../components/SectionTitle";
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

function PercentBars({ title, items }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-soft">
      <h3 className="mb-3 font-semibold text-ink">{title}</h3>
      {items.length === 0 && <p className="text-sm text-ink/50">Sin datos.</p>}
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
      setError(err.response?.data?.message || "No se pudo cargar el reporte.");
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
      setError("No se pudo descargar el reporte.");
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
          eyebrow="Administración"
          title="Reportes"
          description="Filtra la información o déjala vacía para incluir todo, y descarga en PDF o Excel."
        />

        <div className="grid gap-3 md:grid-cols-5">
          <input type="date" value={filters.from} onChange={setField("from")} className={fieldClass} />
          <input type="date" value={filters.to} onChange={setField("to")} className={fieldClass} />
          <select value={filters.status} onChange={setField("status")} className={fieldClass}>
            <option value="">Todos los estados</option>
            <option value="PENDING">Pendiente</option>
            <option value="PAID">Pagado</option>
            <option value="SHIPPED">Enviado</option>
            <option value="CANCELLED">Cancelado</option>
          </select>
          <select value={filters.paymentMethod} onChange={setField("paymentMethod")} className={fieldClass}>
            <option value="">Todos los pagos</option>
            <option value="CASH">Efectivo</option>
            <option value="TRANSFER">Transferencia</option>
            <option value="PAYPAL">PayPal</option>
          </select>
          <select value={filters.category} onChange={setField("category")} className={fieldClass}>
            <option value="">Todas las categorías</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
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
            {loading ? "Calculando..." : "Aplicar filtros"}
          </button>
          <button
            onClick={clearFilters}
            className="rounded-xl border border-ink/20 px-4 py-2 text-sm font-semibold text-ink"
          >
            Limpiar
          </button>
          <button
            onClick={() => handleDownload("PDF")}
            disabled={Boolean(downloading)}
            className="inline-flex items-center gap-2 rounded-xl bg-coral px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            <FileText size={16} /> {downloading === "PDF" ? "Generando..." : "Descargar PDF"}
          </button>
          <button
            onClick={() => handleDownload("XLSX")}
            disabled={Boolean(downloading)}
            className="inline-flex items-center gap-2 rounded-xl bg-mint px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            <FileSpreadsheet size={16} /> {downloading === "XLSX" ? "Generando..." : "Descargar Excel"}
          </button>
        </div>

        {error && <p className="rounded-xl bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}
      </section>

      {summary && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Kpi label="Ingresos" value={formatCurrency(summary.totalRevenue)} />
            <Kpi label="Pedidos vendidos" value={`${summary.soldOrders} / ${summary.totalOrders}`} />
            <Kpi label="Ticket promedio" value={formatCurrency(summary.averageTicket)} />
            <Kpi label="Unidades vendidas" value={summary.unitsSold} />
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <PercentBars title="Pedidos por estado" items={summary.byStatus} />
            <PercentBars title="Ingresos por método de pago" items={summary.byPaymentMethod} />
            <PercentBars title="Ingresos por categoría" items={summary.byCategory} />
          </div>
        </>
      )}
    </div>
  );
}