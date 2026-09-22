import { Printer, X } from "lucide-react";
import { formatCurrency } from "../utils/formatCurrency";

function formatDate(value) {
  return new Date(value).toLocaleString("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  });
}

const paymentLabels = {
  CASH: "Efectivo",
  TRANSFER: "Transferencia",
  PAYPAL: "PayPal",
};

const statusLabels = {
  PENDING: "Pendiente",
  PAID: "Pagado",
  SHIPPED: "Enviado",
  CANCELLED: "Cancelado",
};

export default function TicketModal({ ticket, onClose }) {
  if (!ticket) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/50 p-4 print:static print:bg-transparent print:p-0">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-card print:shadow-none">
        <div className="flex items-center justify-between print:hidden">
          <h3 className="font-display text-2xl text-ink">Ticket de venta</h3>
          <button
            onClick={onClose}
            className="rounded-full p-1 text-ink/60 hover:bg-sand"
            aria-label="Cerrar"
          >
            <X size={20} />
          </button>
        </div>

        <div id="ticket-content" className="mt-4 space-y-3 text-sm text-ink">
          <div className="text-center">
            <p className="font-display text-xl">Huellitas Shop</p>
            <p className="text-xs text-ink/60">Tienda de mascotas</p>
          </div>

          <div className="border-t border-dashed border-ink/30 pt-3">
            <p><span className="font-semibold">Ticket:</span> {ticket.ticketNumber}</p>
            <p><span className="font-semibold">Pedido:</span> #{ticket.orderId}</p>
            <p><span className="font-semibold">Fecha:</span> {formatDate(ticket.orderDate)}</p>
            <p><span className="font-semibold">Cliente:</span> {ticket.customerName}</p>
            <p><span className="font-semibold">Estado:</span> {statusLabels[ticket.status] || ticket.status}</p>
            <p><span className="font-semibold">Pago:</span> {paymentLabels[ticket.paymentMethod] || "Pendiente"}</p>
          </div>

          <div className="border-t border-dashed border-ink/30 pt-3">
            {ticket.items.map((item) => (
              <div key={item.productId} className="flex justify-between py-1">
                <span>
                  {item.quantity} x {item.productName}
                </span>
                <span>{formatCurrency(item.subtotal)}</span>
              </div>
            ))}
          </div>

          <div className="flex justify-between border-t border-dashed border-ink/30 pt-3 text-base font-bold">
            <span>Total</span>
            <span>{formatCurrency(ticket.totalAmount)}</span>
          </div>

          <p className="text-center text-xs text-ink/50">¡Gracias por tu compra!</p>
        </div>

        <button
          onClick={() => window.print()}
          className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream print:hidden"
        >
          <Printer size={16} />
          Imprimir ticket
        </button>
      </div>
    </div>
  );
}
