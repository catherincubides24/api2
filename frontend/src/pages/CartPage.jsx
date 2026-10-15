import { Banknote, CreditCard, Landmark, Minus, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { useLanguage } from "../context/LanguageContext";
import { orderService } from "../services/orderService";
import { paymentService } from "../services/paymentService";
import { formatCurrency } from "../utils/formatCurrency";

export default function CartPage() {
  const { items, total, updateQuantity, removeFromCart, clearCart } = useCart();
  const { user } = useAuth();
  const { t, tProductName } = useLanguage();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [showPaymentOptions, setShowPaymentOptions] = useState(false);

  const paymentMethods = [
    { value: "CASH", label: t("cart.paymentMethods.CASH.label"), icon: Banknote, hint: t("cart.paymentMethods.CASH.hint") },
    { value: "TRANSFER", label: t("cart.paymentMethods.TRANSFER.label"), icon: Landmark, hint: t("cart.paymentMethods.TRANSFER.hint") },
    { value: "PAYPAL", label: t("cart.paymentMethods.PAYPAL.label"), icon: CreditCard, hint: t("cart.paymentMethods.PAYPAL.hint") },
  ];

  const handleSelectPayment = async (method) => {
    if (!user) {
      setFeedback(t("cart.loginRequired"));
      return;
    }

    setIsSubmitting(true);
    setFeedback("");

    try {
      const order = await orderService.createOrder({
        userId: user.id,
        status: "PENDING",
        paymentMethod: method,
        items: items.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
        })),
      });

      if (method === "PAYPAL") {
        sessionStorage.setItem("pendingPayPalOrderId", String(order.id));
        const paypal = await paymentService.createPayPalOrder(order.id);
        window.location.href = paypal.approvalUrl;
        return;
      }

      clearCart();
      navigate("/purchases");
    } catch (err) {
      setFeedback(
        err.response?.data?.message || t("cart.startOrderError")
      );
      setIsSubmitting(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl rounded-3xl bg-white/80 p-8 text-center shadow-soft">
        <h1 className="font-display text-4xl text-ink">{t("cart.emptyTitle")}</h1>
        <p className="mt-3 text-ink/70">
          {t("cart.emptySubtitle")}
        </p>
        <Link
          to="/"
          className="mt-6 inline-block rounded-full bg-ink px-5 py-3 text-sm font-semibold text-cream"
        >
          {t("cart.backToCatalog")}
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <section className="space-y-4 rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow={t("cart.eyebrow")}
          title={t("cart.title")}
          description={t("cart.description")}
        />

        <div className="space-y-4">
          {items.map((item) => (
            <article
              key={item.id}
              className="grid gap-3 rounded-2xl border border-ink/10 bg-white p-4 sm:grid-cols-[90px_1fr_auto] sm:items-center"
            >
              <img
                src={item.imageUrl}
                alt={tProductName(item.name)}
                className="h-20 w-full rounded-xl object-cover sm:w-20"
              />

              <div>
                <h3 className="font-semibold text-ink">{tProductName(item.name)}</h3>
                <p className="text-sm text-ink/60">{formatCurrency(item.price)} {t("cart.each")}</p>
                <p className="text-sm font-semibold text-coral">
                  {t("cart.subtotal")}: {formatCurrency(Number(item.price) * item.quantity)}
                </p>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-center">
                <button
                  onClick={() => updateQuantity(item.id, item.quantity - 1)}
                  className="rounded-full border border-ink/20 p-2 text-ink"
                  aria-label={t("cart.reduceQuantity")}
                >
                  <Minus size={16} />
                </button>
                <span className="min-w-8 text-center text-sm font-semibold">
                  {item.quantity}
                </span>
                <button
                  onClick={() => updateQuantity(item.id, item.quantity + 1)}
                  className="rounded-full border border-ink/20 p-2 text-ink"
                  aria-label={t("cart.increaseQuantity")}
                >
                  <Plus size={16} />
                </button>
                <button
                  onClick={() => removeFromCart(item.id)}
                  className="rounded-full border border-coral/30 p-2 text-coral"
                  aria-label={t("cart.removeProduct")}
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <aside className="h-fit space-y-4 rounded-3xl bg-gradient-to-br from-ink via-dusk to-mint p-6 text-cream shadow-card">
        <h2 className="font-display text-3xl">{t("cart.total")}</h2>
        <p className="text-4xl font-extrabold">{formatCurrency(total)}</p>
        <p className="text-sm text-cream/80">
          {t("cart.customer")}: <span className="font-semibold">{user?.fullName}</span>
        </p>

        {!showPaymentOptions ? (
          <button
            onClick={() => setShowPaymentOptions(true)}
            disabled={!user}
            className="w-full rounded-xl bg-coral px-4 py-3 text-sm font-semibold text-white transition hover:bg-coral/90 disabled:opacity-60"
          >
            {t("cart.checkout")}
          </button>
        ) : (
          <div className="space-y-2">
            <p className="text-sm font-semibold text-cream/90">{t("cart.choosePayment")}</p>
            {paymentMethods.map(({ value, label, icon: Icon, hint }) => (
              <button
                key={value}
                onClick={() => handleSelectPayment(value)}
                disabled={isSubmitting}
                className="flex w-full items-center gap-3 rounded-xl bg-white/10 px-4 py-3 text-left text-sm transition hover:bg-white/20 disabled:opacity-60"
              >
                <Icon size={18} />
                <span>
                  <span className="block font-semibold">{label}</span>
                  <span className="block text-xs text-cream/70">{hint}</span>
                </span>
              </button>
            ))}
            <button
              onClick={() => setShowPaymentOptions(false)}
              disabled={isSubmitting}
              className="w-full rounded-xl border border-white/30 px-4 py-2 text-xs font-semibold text-cream/80"
            >
              {t("cart.back")}
            </button>
          </div>
        )}

        {feedback && (
          <p className="rounded-xl bg-white/15 px-3 py-2 text-sm text-cream">{feedback}</p>
        )}

        <button
          onClick={clearCart}
          className="w-full rounded-xl border border-white/35 px-4 py-3 text-sm font-semibold text-cream/90 transition hover:bg-white/10"
        >
          {t("cart.clearCart")}
        </button>
      </aside>
    </div>
  );
}
