import { Printer, X } from "lucide-react";
import { useLanguage } from "../context/LanguageContext";
import { formatCurrency } from "../utils/formatCurrency";

export default function TicketModal({ ticket, onClose }) {
  const { t, formatDate, tProductName } = useLanguage();
  if (!ticket) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/50 p-4 print:static print:bg-transparent print:p-0">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-card print:shadow-none">
        <div className="flex items-center justify-between print:hidden">
          <h3 className="font-display text-2xl text-ink">{t("ticket.title")}</h3>
          <button
            onClick={onClose}
            className="rounded-full p-1 text-ink/60 hover:bg-sand"
            aria-label={t("ticket.close")}
          >
            <X size={20} />
          </button>
        </div>

        <div id="ticket-content" className="mt-4 space-y-3 text-sm text-ink">
          <div className="text-center">
            <p className="font-display text-xl">Huellitas Shop</p>
            <p className="text-xs text-ink/60">{t("ticket.petStore")}</p>
          </div>

          <div className="border-t border-dashed border-ink/30 pt-3">
            <p><span className="font-semibold">{t("ticket.ticketNumber")}</span> {ticket.ticketNumber}</p>
            <p><span className="font-semibold">{t("ticket.order")}</span> #{ticket.orderId}</p>
            <p><span className="font-semibold">{t("ticket.date")}</span> {formatDate(ticket.orderDate, { dateStyle: "medium", timeStyle: "short" })}</p>
            <p><span className="font-semibold">{t("ticket.customer")}</span> {ticket.customerName}</p>
            <p><span className="font-semibold">{t("ticket.status")}</span> {t(`orderStatus.${ticket.status}`) || ticket.status}</p>
            <p><span className="font-semibold">{t("ticket.payment")}</span> {t(`paymentMethods.${ticket.paymentMethod}`) || t("orderStatus.PENDING")}</p>
          </div>

          <div className="border-t border-dashed border-ink/30 pt-3">
            {ticket.items.map((item) => (
              <div key={item.productId} className="flex justify-between py-1">
                <span>
                  {item.quantity} x {tProductName(item.productName)}
                </span>
                <span>{formatCurrency(item.subtotal)}</span>
              </div>
            ))}
          </div>

          <div className="flex justify-between border-t border-dashed border-ink/30 pt-3 text-base font-bold">
            <span>{t("ticket.total")}</span>
            <span>{formatCurrency(ticket.totalAmount)}</span>
          </div>

          <p className="text-center text-xs text-ink/50">{t("ticket.thankYou")}</p>
        </div>

        <button
          onClick={() => window.print()}
          className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream print:hidden"
        >
          <Printer size={16} />
          {t("ticket.print")}
        </button>
      </div>
    </div>
  );
}
