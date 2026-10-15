import { useEffect, useMemo, useState } from "react";
import { Banknote, CreditCard, Landmark } from "lucide-react";
import SectionTitle from "../components/SectionTitle";
import TicketModal from "../components/TicketModal";
import { useLanguage } from "../context/LanguageContext";
import { orderService } from "../services/orderService";
import { paymentService } from "../services/paymentService";
import { productService } from "../services/productService";
import { userService } from "../services/userService";
import { formatCurrency } from "../utils/formatCurrency";

export default function AdminSalesPage() {
  const { t, tProductName } = useLanguage();
  const [products, setProducts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [search, setSearch] = useState("");
  const [saleItems, setSaleItems] = useState([]);
  const [customerId, setCustomerId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [ticket, setTicket] = useState(null);

  const paymentOptions = [
    { value: "CASH", label: t("cart.paymentMethods.CASH.label"), icon: Banknote },
    { value: "TRANSFER", label: t("cart.paymentMethods.TRANSFER.label"), icon: Landmark },
    { value: "PAYPAL", label: t("cart.paymentMethods.PAYPAL.label"), icon: CreditCard },
  ];

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        const [productData, userData] = await Promise.all([
          productService.getAdminProducts(),
          userService.getAllUsers(),
        ]);
        setProducts(productData);
        setCustomers(userData.filter((u) => u.role === "CUSTOMER"));
      } catch (err) {
        setFeedback(
          err.response?.data?.message || t("adminSales.initialDataError")
        );
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [t]);

  const filteredProducts = useMemo(() => {
    return products.filter(
      (product) =>
        product.active &&
        product.stock > 0 &&
        product.name.toLowerCase().includes(search.toLowerCase())
    );
  }, [products, search]);

  const total = useMemo(
    () => saleItems.reduce((acc, item) => acc + item.price * item.quantity, 0),
    [saleItems]
  );

  const addProduct = (product) => {
    setSaleItems((current) => {
      const existing = current.find((item) => item.productId === product.id);
      if (existing) {
        return current.map((item) =>
          item.productId === product.id
            ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
            : item
        );
      }
      return [
        ...current,
        {
          productId: product.id,
          name: product.name,
          price: Number(product.price),
          quantity: 1,
          stock: product.stock,
        },
      ];
    });
  };

  const updateQuantity = (productId, quantity) => {
    setSaleItems((current) =>
      current
        .map((item) => (item.productId === productId ? { ...item, quantity } : item))
        .filter((item) => item.quantity > 0)
    );
  };

  const removeItem = (productId) => {
    setSaleItems((current) => current.filter((item) => item.productId !== productId));
  };

  const resetSale = () => {
    setSaleItems([]);
    setCustomerId("");
    setPaymentMethod("CASH");
  };

  const handleFinalizeSale = async () => {
    if (!customerId) {
      setFeedback(t("adminSales.customerRequired"));
      return;
    }
    if (saleItems.length === 0) {
      setFeedback(t("adminSales.itemsRequired"));
      return;
    }

    setSubmitting(true);
    setFeedback("");

    try {
      const order = await orderService.createOrder({
        userId: Number(customerId),
        status: "PENDING",
        paymentMethod,
        items: saleItems.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
      });

      if (paymentMethod === "PAYPAL") {
        const paypal = await paymentService.createPayPalOrder(order.id);
        window.location.href = paypal.approvalUrl;
        return;
      }

      await orderService.updatePayment(order.id, paymentMethod, true);
      const ticketData = await orderService.getTicket(order.id);
      setTicket(ticketData);
      setFeedback(t("adminSales.saleSuccess", { id: order.id }));
      resetSale();
    } catch (err) {
      setFeedback(err.response?.data?.message || t("adminSales.saleError"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow={t("adminSales.eyebrow")}
          title={t("adminSales.title")}
          description={t("adminSales.description")}
        />
      </section>

      {feedback && (
        <div className="rounded-2xl border border-mint/30 bg-mint/15 px-4 py-3 text-sm text-dusk">
          {feedback}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="space-y-4 rounded-3xl bg-white/85 p-6 shadow-card">
          <input
            type="text"
            placeholder={t("adminSales.searchPlaceholder")}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-full rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 focus:ring"
          />

          {loading ? (
            <p className="text-sm text-ink/60">{t("adminSales.loadingCatalog")}</p>
          ) : (
            <div className="grid max-h-96 gap-2 overflow-y-auto sm:grid-cols-2">
              {filteredProducts.map((product) => (
                <button
                  key={product.id}
                  onClick={() => addProduct(product)}
                  className="rounded-xl border border-ink/10 bg-white p-3 text-left transition hover:border-coral/40"
                >
                  <p className="text-sm font-semibold text-ink">{tProductName(product.name)}</p>
                  <p className="text-xs text-ink/60">
                    {formatCurrency(product.price)} · {t("product.stock", { count: product.stock })}
                  </p>
                </button>
              ))}
              {filteredProducts.length === 0 && (
                <p className="col-span-2 text-sm text-ink/60">
                  {t("adminSales.noProductsFound")}
                </p>
              )}
            </div>
          )}
        </section>

        <aside className="space-y-4 rounded-3xl bg-white/85 p-6 shadow-card">
          <h3 className="font-display text-2xl text-ink">{t("adminSales.currentSaleTitle")}</h3>

          <div>
            <label className="mb-1 block text-sm font-semibold text-ink/70">{t("adminSales.selectCustomer")}</label>
            <select
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
              className="w-full rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 focus:ring"
            >
              <option value="">{t("adminSales.selectCustomerOption")}</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.fullName} ({customer.email})
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            {saleItems.length === 0 && (
              <p className="text-sm text-ink/60">{t("adminSales.noProductsAdded")}</p>
            )}
            {saleItems.map((item) => (
              <div
                key={item.productId}
                className="flex items-center justify-between gap-2 rounded-xl bg-sand/70 px-3 py-2"
              >
                <div>
                  <p className="text-sm font-semibold text-ink">{tProductName(item.name)}</p>
                  <p className="text-xs text-ink/60">{formatCurrency(item.price)} {t("adminSales.each")}</p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min={1}
                    max={item.stock}
                    value={item.quantity}
                    onChange={(event) =>
                      updateQuantity(item.productId, Number(event.target.value))
                    }
                    className="w-14 rounded-lg border border-ink/20 px-2 py-1 text-center text-sm"
                  />
                  <button
                    onClick={() => removeItem(item.productId)}
                    className="text-xs font-semibold text-coral"
                  >
                    {t("adminSales.remove")}
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div>
            <label className="mb-1 block text-sm font-semibold text-ink/70">{t("adminSales.paymentMethod")}</label>
            <div className="grid grid-cols-3 gap-2">
              {paymentOptions.map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  onClick={() => setPaymentMethod(value)}
                  className={[
                    "flex flex-col items-center gap-1 rounded-xl border px-2 py-2 text-xs font-semibold",
                    paymentMethod === value
                      ? "border-ink bg-ink text-cream"
                      : "border-ink/15 text-ink/70",
                  ].join(" ")}
                >
                  <Icon size={16} />
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between border-t border-ink/10 pt-3">
            <span className="font-semibold text-ink">{t("adminSales.total")}</span>
            <span className="text-2xl font-extrabold text-coral">{formatCurrency(total)}</span>
          </div>

          <button
            onClick={handleFinalizeSale}
            disabled={submitting}
            className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream disabled:opacity-60"
          >
            {submitting ? t("adminSales.processing") : t("adminSales.finalizeSale")}
          </button>
        </aside>
      </div>

      <TicketModal ticket={ticket} onClose={() => setTicket(null)} />
    </div>
  );
}
