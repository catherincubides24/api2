import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useLanguage } from "../context/LanguageContext";
import { paymentService } from "../services/paymentService";

export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const { clearCart } = useCart();
  const { t } = useLanguage();

  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const paypalOrderId = searchParams.get("token");
    const orderId = sessionStorage.getItem("pendingPayPalOrderId");

    if (!paypalOrderId || !orderId) {
      setStatus("error");
      setMessage(t("paymentSuccess.missingData"));
      return;
    }

    const capture = async () => {
      try {
        const result = await paymentService.capturePayPalOrder(
          paypalOrderId,
          Number(orderId)
        );

        if (result.status === "COMPLETED") {
          clearCart();
          sessionStorage.removeItem("pendingPayPalOrderId");
          setStatus("success");
          setMessage(result.message);
        } else {
          setStatus("error");
          setMessage(t("paymentSuccess.notCompletedStatus", { status: result.status }));
        }
      } catch (err) {
        setStatus("error");
        setMessage(err.response?.data?.message || t("paymentSuccess.confirmError"));
      }
    };

    capture();
  }, [clearCart, searchParams, t]);

  return (
    <div className="mx-auto max-w-2xl rounded-3xl bg-white/85 p-10 text-center shadow-card">
      {status === "loading" && (
        <>
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-mint border-t-transparent" />
          <h1 className="font-display text-3xl text-ink">{t("paymentSuccess.confirming")}</h1>
          <p className="mt-2 text-ink/70">{t("paymentSuccess.waitMoment")}</p>
        </>
      )}

      {status === "success" && (
        <>
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-mint/20 text-mint text-3xl">
            ✓
          </div>
          <h1 className="font-display text-4xl text-ink">{t("paymentSuccess.title")}</h1>
          <p className="mt-3 text-ink/70">{message}</p>
          <Link
            to="/purchases"
            className="mt-6 inline-block rounded-full bg-ink px-6 py-3 text-sm font-semibold text-cream"
          >
            {t("paymentSuccess.viewOrders")}
          </Link>
        </>
      )}

      {status === "error" && (
        <>
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-coral/20 text-coral text-3xl">
            ✕
          </div>
          <h1 className="font-display text-4xl text-ink">{t("paymentSuccess.failedTitle")}</h1>
          <p className="mt-3 text-ink/70">{message}</p>
          <Link
            to="/cart"
            className="mt-6 inline-block rounded-full bg-ink px-6 py-3 text-sm font-semibold text-cream"
          >
            {t("paymentSuccess.backToCart")}
          </Link>
        </>
      )}
    </div>
  );
}