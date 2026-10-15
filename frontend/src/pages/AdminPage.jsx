import { useEffect, useMemo, useState } from "react";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { productService } from "../services/productService";
import { formatCurrency } from "../utils/formatCurrency";

const initialForm = {
  name: "",
  description: "",
  price: "",
  stock: "",
  imageUrl: "",
  category: "",
  active: true,
};

const inputClass =
  "rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream";

export default function AdminPage() {
  const { isAdmin } = useAuth();
  const { t, tProductName, tProductDesc, tCategory } = useLanguage();
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  const isEditing = useMemo(() => editingId !== null, [editingId]);

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await productService.getAdminProducts();
      setProducts(data);
    } catch (err) {
      setMessage(err.response?.data?.message || t("adminProducts.loadError"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const resetForm = () => {
    setForm(initialForm);
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    const payload = {
      ...form,
      price: Number(form.price),
      stock: Number(form.stock),
    };

    try {
      if (isEditing) {
        await productService.updateProduct(editingId, payload);
        setMessage(t("adminProducts.updatedFeedback"));
      } else {
        await productService.createProduct(payload);
        setMessage(t("adminProducts.createdFeedback"));
      }

      resetForm();
      await loadProducts();
    } catch (err) {
      setMessage(err.response?.data?.message || t("adminProducts.saveError"));
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (product) => {
    setEditingId(product.id);
    setForm({
      name: product.name,
      description: product.description || "",
      price: product.price,
      stock: product.stock,
      imageUrl: product.imageUrl || "",
      category: product.category || "",
      active: product.active,
    });

    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDelete = async (id) => {
    if (!window.confirm(t("adminProducts.deleteConfirm"))) return;

    try {
      await productService.deleteProduct(id);
      setMessage(t("adminProducts.deletedFeedback"));
      await loadProducts();
    } catch (err) {
      setMessage(
        err.response?.data?.message || t("adminProducts.deleteError")
      );
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card dark:bg-slate-800/90">
        <SectionTitle
          eyebrow={t("adminProducts.eyebrow")}
          title={isEditing ? t("adminProducts.titleEdit") : t("adminProducts.titleCreate")}
          description={t("adminProducts.description")}
        />

        <form onSubmit={handleSubmit} className="mt-6 grid gap-3 md:grid-cols-2">
          <input
            required
            placeholder={t("adminProducts.form.name")}
            value={form.name}
            onChange={(event) =>
              setForm((current) => ({ ...current, name: event.target.value }))
            }
            className={inputClass}
          />
          <input
            required
            type="number"
            step="0.01"
            min="0.01"
            placeholder={t("adminProducts.form.price")}
            value={form.price}
            onChange={(event) =>
              setForm((current) => ({ ...current, price: event.target.value }))
            }
            className={inputClass}
          />
          <input
            required
            type="number"
            min="0"
            placeholder={t("adminProducts.form.stock")}
            value={form.stock}
            onChange={(event) =>
              setForm((current) => ({ ...current, stock: event.target.value }))
            }
            className={inputClass}
          />
          <input
            placeholder={t("adminProducts.form.category")}
            value={form.category}
            onChange={(event) =>
              setForm((current) => ({ ...current, category: event.target.value }))
            }
            className={inputClass}
          />
          <input
            placeholder={t("adminProducts.form.imageUrl")}
            value={form.imageUrl}
            onChange={(event) =>
              setForm((current) => ({ ...current, imageUrl: event.target.value }))
            }
            className={`md:col-span-2 ${inputClass}`}
          />
          <textarea
            placeholder={t("adminProducts.form.description")}
            value={form.description}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                description: event.target.value,
              }))
            }
            rows={3}
            className={`md:col-span-2 ${inputClass}`}
          />

          <label className="inline-flex items-center gap-2 text-sm font-semibold text-ink/80 dark:text-cream/80">
            <input
              type="checkbox"
              checked={Boolean(form.active)}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  active: event.target.checked,
                }))
              }
            />
            {t("adminProducts.form.active")}
          </label>

          <div className="flex items-center gap-2 md:justify-end">
            {isEditing && (
              <button
                type="button"
                onClick={resetForm}
                className="rounded-xl border border-ink/20 px-4 py-2.5 text-sm font-semibold text-ink dark:border-white/20 dark:text-cream"
              >
                {t("adminProducts.form.cancelEdit")}
              </button>
            )}
            <button
              type="submit"
              disabled={saving}
              className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-cream disabled:opacity-60 dark:bg-cream dark:text-ink"
            >
              {saving
                ? t("adminProducts.form.saving")
                : isEditing
                  ? t("adminProducts.form.update")
                  : t("adminProducts.form.create")}
            </button>
          </div>
        </form>

        {message && (
          <div className="mt-4 rounded-xl bg-sand px-4 py-3 text-sm text-ink/80 dark:bg-slate-700 dark:text-cream/85">
            {message}
          </div>
        )}
      </section>

      <section className="rounded-3xl bg-white/85 p-6 shadow-card dark:bg-slate-800/90">
        <SectionTitle
          eyebrow={t("adminProducts.currentProductsEyebrow")}
          title={t("adminProducts.currentProductsTitle")}
          description={t("adminProducts.currentProductsDesc")}
        />

        {loading ? (
          <p className="mt-4 text-sm text-ink/70 dark:text-cream/70">{t("adminProducts.loading")}</p>
        ) : (
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {products.map((product) => (
              <article
                key={product.id}
                className="rounded-2xl border border-ink/10 bg-white p-4 dark:border-white/10 dark:bg-slate-900"
              >
                <h3 className="font-semibold text-ink dark:text-cream">{tProductName(product.name)}</h3>
                <p className="text-sm text-ink/65 line-clamp-2 dark:text-cream/65">
                  {tProductDesc(product.name, product.description)}
                </p>
                <p className="mt-2 text-sm font-semibold text-coral">
                  {formatCurrency(product.price)} - {t("product.stock", { count: product.stock })}
                </p>
                <p className="text-xs text-ink/60 dark:text-cream/60">
                  {t("adminProducts.status")} {product.active ? t("product.active") : t("product.inactive")}
                </p>

                <div className="mt-3 flex gap-2">
                  <button
                    onClick={() => handleEdit(product)}
                    className="rounded-xl border border-ink/20 px-3 py-2 text-xs font-semibold text-ink dark:border-white/20 dark:text-cream"
                  >
                    {t("adminProducts.edit")}
                  </button>
                  {isAdmin && (
                    <button
                      onClick={() => handleDelete(product.id)}
                      className="rounded-xl border border-coral/30 px-3 py-2 text-xs font-semibold text-coral"
                    >
                      {t("adminProducts.delete")}
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}