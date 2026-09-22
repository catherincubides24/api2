import { ReceiptText } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Pagination from "../components/Pagination";
import SectionTitle from "../components/SectionTitle";
import TicketModal from "../components/TicketModal";
import { useAuth } from "../context/AuthContext";
import { orderService } from "../services/orderService";
import { formatCurrency } from "../utils/formatCurrency";

const statusStyles = {
  PENDING: "bg-amber-100 text-amber-700",
  PAID: "bg-emerald-100 text-emerald-700",
  SHIPPED: "bg-sky-100 text-sky-700",
  CANCELLED: "bg-rose-100 text-rose-700",
};

const statusLabels = {
  PENDING: "Pendiente",
  PAID: "Pagado",
  SHIPPED: "Enviado",
  CANCELLED: "Cancelado",
};

function formatDate(value) {
  return new Date(value).toLocaleString("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  });
}

/**
 * Historial de compras del cliente autenticado (paginado).
 * La gestión administrativa de todos los pedidos vive ahora en AdminOrdersPage,
 * para que cada cliente solo vea su propia información.
 */
export default function PurchasesPage() {
  const { user } = useAuth();
  const [pageData, setPageData] = useState({ content: [], page: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [ticket, setTicket] = useState(null);
  const pageSize = 5;

  const loadOrders = async (page = 0) => {
    if (!user?.id) return;
    try {
      setLoading(true);
      setError("");
      const data = await orderService.getOrdersByUser(user.id, page, pageSize);
      setPageData(data);
    } catch (err) {
      setError(
        err.response?.data?.message || "No se pudo cargar tu historial de compras."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const handleShowTicket = async (orderId) => {
    try {
      const data = await orderService.getTicket(orderId);
      setTicket(data);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo obtener el ticket.");
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Pedidos"
          title="Mis compras"
          description="Aquí puedes ver todas las compras que ya realizaste en la tienda."
        />
      </section>

      {loading && (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-36 animate-pulse rounded-2xl bg-white/80" />
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm text-coral">
          {error}
        </div>
      )}

      {!loading && !error && pageData.content.length === 0 && (
        <div className="rounded-3xl bg-white/85 p-8 text-center shadow-soft">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-sand text-ink">
            <ReceiptText size={24} />
          </div>
          <h2 className="font-display text-3xl text-ink">Aún no tienes compras</h2>
          <p className="mt-2 text-ink/70">
            Cuando completes un pedido desde el carrito, aparecerá aquí.
          </p>
          <Link
            to="/"
            className="mt-6 inline-flex rounded-full bg-ink px-5 py-3 text-sm font-semibold text-cream"
          >
            Ir al catálogo
          </Link>
        </div>
      )}

      {!loading && !error && pageData.content.length > 0 && (
        <div className="space-y-4">
          {pageData.content.map((order) => (
            <article
              key={order.id}
              className="rounded-3xl border border-white/75 bg-white/90 p-5 shadow-soft"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink/10 pb-4">
                <div>
                  <p className="text-xs uppercase tracking-[0.2em] text-ink/50">
                    Pedido #{order.id} · {order.ticketNumber}
                  </p>
                  <h3 className="text-lg font-bold text-ink">{formatDate(order.createdAt)}</h3>
                </div>

                <div className="text-right">
                  <span
                    className={[
                      "inline-flex rounded-full px-3 py-1 text-xs font-semibold",
                      statusStyles[order.status] || "bg-slate-100 text-slate-700",
                    ].join(" ")}
                  >
                    {statusLabels[order.status] || order.status}
                  </span>
                  <p className="mt-2 text-xl font-extrabold text-coral">
                    {formatCurrency(order.totalAmount)}
                  </p>
                </div>
              </div>

              <div className="mt-4 space-y-2">
                {order.items?.map((item) => (
                  <div
                    key={`${order.id}-${item.productId}`}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-sand/70 px-3 py-2"
                  >
                    <p className="text-sm font-semibold text-ink">{item.productName}</p>
                    <p className="text-sm text-ink/75">
                      {item.quantity} x {formatCurrency(item.unitPrice)} ={" "}
                      <span className="font-semibold text-ink">
                        {formatCurrency(item.subtotal)}
                      </span>
                    </p>
                  </div>
                ))}
              </div>

              <button
                onClick={() => handleShowTicket(order.id)}
                className="mt-4 inline-flex items-center gap-2 rounded-full border border-ink/20 px-4 py-2 text-xs font-semibold text-ink hover:bg-sand"
              >
                <ReceiptText size={14} />
                Ver ticket
              </button>
            </article>
          ))}

          <Pagination
            page={pageData.page}
            totalPages={pageData.totalPages}
            onPageChange={(page) => loadOrders(page)}
          />
        </div>
      )}

      <TicketModal ticket={ticket} onClose={() => setTicket(null)} />
    </div>
  );
}
