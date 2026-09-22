import { useEffect, useState } from "react";
import { ReceiptText } from "lucide-react";
import Pagination from "../components/Pagination";
import SectionTitle from "../components/SectionTitle";
import TicketModal from "../components/TicketModal";
import { orderService } from "../services/orderService";
import { formatCurrency } from "../utils/formatCurrency";

const statusLabels = {
  PENDING: "Pendiente",
  PAID: "Pagado",
  SHIPPED: "Enviado",
  CANCELLED: "Cancelado",
};

const paymentLabels = {
  CASH: "Efectivo",
  TRANSFER: "Transferencia",
  PAYPAL: "PayPal",
};

const statusStyles = {
  PENDING: "bg-amber-100 text-amber-700",
  PAID: "bg-emerald-100 text-emerald-700",
  SHIPPED: "bg-sky-100 text-sky-700",
  CANCELLED: "bg-rose-100 text-rose-700",
};

function formatDate(value) {
  return new Date(value).toLocaleDateString("es-CO", {
    dateStyle: "medium",
    timeZone: "America/Bogota",
  });
}

/**
 * Vista compacta en tabla para que el administrador gestione los pedidos:
 * fecha, cliente, N° de ticket, método de pago, estado y acciones rápidas
 * (cambiar estado, ver/imprimir ticket). Con paginación desde el backend.
 */
export default function AdminOrdersPage() {
  const [pageData, setPageData] = useState({ content: [], page: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState("");
  const [ticket, setTicket] = useState(null);
  const pageSize = 8;

  const loadOrders = async (page = 0) => {
    try {
      setLoading(true);
      setError("");
      const data = await orderService.getAllOrders(page, pageSize);
      setPageData(data);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudieron cargar los pedidos.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders(0);
  }, []);

  const handleStatusChange = async (orderId, status) => {
    try {
      await orderService.updateOrderStatus(orderId, status);
      setFeedback(`Pedido #${orderId} actualizado a ${statusLabels[status]}.`);
      await loadOrders(pageData.page);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo actualizar el estado.");
    }
  };

  const handleConfirmPayment = async (orderId, paymentMethod) => {
    try {
      await orderService.updatePayment(orderId, paymentMethod, true);
      setFeedback(`Pago del pedido #${orderId} confirmado.`);
      await loadOrders(pageData.page);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo confirmar el pago.");
    }
  };

  const handleShowTicket = async (orderId) => {
    try {
      const data = await orderService.getTicket(orderId);
      setTicket(data);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo generar el ticket.");
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Administración"
          title="Pedidos"
          description="Consulta y gestiona el estado de los pedidos realizados por los clientes."
        />
      </section>

      {feedback && (
        <div className="rounded-2xl border border-mint/30 bg-mint/15 px-4 py-3 text-sm text-dusk">
          {feedback}
        </div>
      )}
      {error && (
        <div className="rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm text-coral">
          {error}
        </div>
      )}

      <section className="overflow-x-auto rounded-3xl bg-white/85 p-4 shadow-card">
        {loading ? (
          <p className="p-4 text-sm text-ink/60">Cargando pedidos...</p>
        ) : pageData.content.length === 0 ? (
          <p className="p-4 text-sm text-ink/60">No hay pedidos registrados.</p>
        ) : (
          <table className="w-full min-w-[820px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink/10 text-xs uppercase tracking-wide text-ink/50">
                <th className="px-3 py-2">Fecha</th>
                <th className="px-3 py-2">Cliente</th>
                <th className="px-3 py-2">Ticket</th>
                <th className="px-3 py-2">Pago</th>
                <th className="px-3 py-2">Estado</th>
                <th className="px-3 py-2 text-right">Total</th>
                <th className="px-3 py-2">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {pageData.content.map((order) => (
                <tr key={order.id} className="border-b border-ink/5 last:border-0">
                  <td className="px-3 py-2 text-ink/80">{formatDate(order.createdAt)}</td>
                  <td className="px-3 py-2">
                    <p className="font-semibold text-ink">{order.userName}</p>
                    <p className="text-xs text-ink/50">ID {order.userId}</p>
                  </td>
                  <td className="px-3 py-2 text-ink/80">{order.ticketNumber || "—"}</td>
                  <td className="px-3 py-2 text-ink/80">
                    {order.paymentMethod ? (
                      paymentLabels[order.paymentMethod]
                    ) : (
                      <div className="flex flex-col gap-1">
                        <span className="text-xs text-ink/50">Sin confirmar</span>
                        <div className="flex gap-1">
                          <button
                            onClick={() => handleConfirmPayment(order.id, "CASH")}
                            className="rounded-full border border-ink/15 px-2 py-0.5 text-[11px] font-semibold text-ink hover:bg-sand"
                          >
                            Efectivo
                          </button>
                          <button
                            onClick={() => handleConfirmPayment(order.id, "TRANSFER")}
                            className="rounded-full border border-ink/15 px-2 py-0.5 text-[11px] font-semibold text-ink hover:bg-sand"
                          >
                            Transf.
                          </button>
                        </div>
                      </div>
                    )}
                  </td>
                  <td className="px-3 py-2">
                    <select
                      value={order.status}
                      onChange={(event) => handleStatusChange(order.id, event.target.value)}
                      className={[
                        "rounded-full border-0 px-2 py-1 text-xs font-semibold",
                        statusStyles[order.status] || "bg-slate-100 text-slate-700",
                      ].join(" ")}
                    >
                      {Object.entries(statusLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-3 py-2 text-right font-semibold text-coral">
                    {formatCurrency(order.totalAmount)}
                  </td>
                  <td className="px-3 py-2">
                    <button
                      onClick={() => handleShowTicket(order.id)}
                      className="inline-flex items-center gap-1 rounded-full border border-ink/20 px-3 py-1.5 text-xs font-semibold text-ink hover:bg-sand"
                    >
                      <ReceiptText size={14} />
                      Ticket
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          onPageChange={(page) => loadOrders(page)}
        />
      </section>

      <TicketModal ticket={ticket} onClose={() => setTicket(null)} />
    </div>
  );
}
