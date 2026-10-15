import { ReceiptText } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Pagination from "../components/Pagination";
import SectionTitle from "../components/SectionTitle";
import TicketModal from "../components/TicketModal";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { orderService } from "../services/orderService";
import { formatCurrency } from "../utils/formatCurrency";

const statusStyles = {
  PENDING: "bg-amber-100 text-amber-700",
  PAID: "bg-emerald-100 text-emerald-700",
  SHIPPED: "bg-sky-100 text-sky-700",
  CANCELLED: "bg-rose-100 text-rose-700",
};

export default function PurchasesPage() {
  const { user } = useAuth();
  const { t, formatDate, tProductName } = useLanguage();
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
        err.response?.data?.message || t("purchases.loadError")
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
      setError(err.response?.data?.message || t("purchases.ticketError"));
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow={t("purchases.eyebrow")}
          title={t("purchases.title")}
          description={t("purchases.description")}
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
          <h2 className="font-display text-3xl text-ink">{t("purchases.emptyTitle")}</h2>
          <p className="mt-2 text-ink/70">
            {t("purchases.emptySubtitle")}
          </p>
          <Link
            to="/"
            className="mt-6 inline-flex rounded-full bg-ink px-5 py-3 text-sm font-semibold text-cream"
          >
            {t("purchases.goToCatalog")}
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
                    {t("purchases.orderNumber", { id: order.id, ticketNumber: order.ticketNumber })}
                  </p>
                  <h3 className="text-lg font-bold text-ink">
                    {formatDate(order.createdAt, { dateStyle: "medium", timeStyle: "short" })}
                  </h3>
                </div>

                <div className="text-right">
                  <span
                    className={[
                      "inline-flex rounded-full px-3 py-1 text-xs font-semibold",
                      statusStyles[order.status] || "bg-slate-100 text-slate-700",
                    ].join(" ")}
                  >
                    {t(`orderStatus.${order.status}`)}
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
                    <p className="text-sm font-semibold text-ink">{tProductName(item.productName)}</p>
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
                {t("purchases.viewTicket")}
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
