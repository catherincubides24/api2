import { ReceiptText } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import SectionTitle from "../components/SectionTitle";
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

const statusOptions = ["PENDING", "PAID", "SHIPPED", "CANCELLED"];

function formatDate(value) {
  return new Date(value).toLocaleString("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  });
}

export default function PurchasesPage() {
  const { user, isAdmin } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState("");
  const [statusDrafts, setStatusDrafts] = useState({});
  const [updatingOrderId, setUpdatingOrderId] = useState(null);

  const fetchOrders = useCallback(async () => {
    if (!user?.id) return;

    try {
      setLoading(true);
      setError("");
      const data = isAdmin
        ? await orderService.getAllOrders()
        : await orderService.getOrdersByUser(user.id);
      setOrders(data);
      setStatusDrafts(
        Object.fromEntries(data.map((order) => [order.id, order.status]))
      );
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "No se pudo cargar tu historial de compras."
      );
    } finally {
      setLoading(false);
    }
  }, [isAdmin, user?.id]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleStatusUpdate = async (orderId) => {
    const nextStatus = statusDrafts[orderId];
    if (!nextStatus) return;

    try {
      setUpdatingOrderId(orderId);
      setFeedback("");
      await orderService.updateOrderStatus(orderId, nextStatus);
      setFeedback(`Estado del pedido #${orderId} actualizado correctamente.`);
      await fetchOrders();
    } catch (err) {
      setFeedback(
        err.response?.data?.message ||
          "No se pudo actualizar el estado del pedido."
      );
    } finally {
      setUpdatingOrderId(null);
    }
  };

  const sortedOrders = useMemo(() => {
    return [...orders].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }, [orders]);

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Pedidos"
          title={isAdmin ? "Todas las compras" : "Mis compras"}
          description={
            isAdmin
              ? "Vista administrativa con todos los pedidos registrados."
              : "Aquí puedes ver todas las compras que ya realizaste en la tienda."
          }
        />
        {feedback && (
          <div className="mt-4 rounded-xl border border-mint/35 bg-mint/15 px-4 py-3 text-sm text-dusk">
            {feedback}
          </div>
        )}
      </section>

      {loading && (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <div
              key={index}
              className="h-36 animate-pulse rounded-2xl bg-white/80"
            />
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm text-coral">
          {error}
        </div>
      )}

      {!loading && !error && sortedOrders.length === 0 && (
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

      {!loading && !error && sortedOrders.length > 0 && (
        <div className="space-y-4">
          {sortedOrders.map((order) => (
            <article
              key={order.id}
              className="rounded-3xl border border-white/75 bg-white/90 p-5 shadow-soft"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink/10 pb-4">
                <div>
                  <p className="text-xs uppercase tracking-[0.2em] text-ink/50">
                    Pedido #{order.id}
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
                {isAdmin && (
                  <div className="space-y-2 rounded-xl border border-ink/10 bg-white p-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.15em] text-ink/55">
                      Cliente: {order.userName} (ID {order.userId})
                    </p>
                    <div className="flex flex-wrap items-center gap-2">
                      <label className="text-sm font-semibold text-ink/70">
                        Estado del pedido:
                      </label>
                      <select
                        value={statusDrafts[order.id] || order.status}
                        onChange={(event) =>
                          setStatusDrafts((current) => ({
                            ...current,
                            [order.id]: event.target.value,
                          }))
                        }
                        className="rounded-lg border border-ink/20 bg-white px-3 py-2 text-sm outline-none ring-coral/30 focus:ring"
                      >
                        {statusOptions.map((status) => (
                          <option key={status} value={status}>
                            {statusLabels[status]}
                          </option>
                        ))}
                      </select>
                      <button
                        onClick={() => handleStatusUpdate(order.id)}
                        disabled={updatingOrderId === order.id}
                        className="rounded-lg bg-ink px-3 py-2 text-sm font-semibold text-cream transition hover:bg-dusk disabled:opacity-60"
                      >
                        {updatingOrderId === order.id ? "Guardando..." : "Actualizar"}
                      </button>
                    </div>
                  </div>
                )}
                {order.items?.map((item) => (
                  <div
                    key={`${order.id}-${item.productId}`}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-sand/70 px-3 py-2"
                  >
                    <p className="text-sm font-semibold text-ink">{item.productName}</p>
                    <p className="text-sm text-ink/75">
                      {item.quantity} x {formatCurrency(item.unitPrice)} = {" "}
                      <span className="font-semibold text-ink">
                        {formatCurrency(item.subtotal)}
                      </span>
                    </p>
                  </div>
                ))}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
