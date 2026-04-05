import { useEffect, useMemo, useState } from "react";
import SectionTitle from "../components/SectionTitle";
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

export default function AdminPage() {
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
      setMessage(err.response?.data?.message || "No se pudieron cargar productos");
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
        setMessage("Producto actualizado correctamente");
      } else {
        await productService.createProduct(payload);
        setMessage("Producto creado correctamente");
      }

      resetForm();
      await loadProducts();
    } catch (err) {
      setMessage(err.response?.data?.message || "No se pudo guardar el producto");
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
    if (!window.confirm("¿Seguro que deseas eliminar este producto?")) return;

    try {
      await productService.deleteProduct(id);
      setMessage("Producto eliminado correctamente");
      await loadProducts();
    } catch (err) {
      setMessage(
        err.response?.data?.message || "No se pudo eliminar el producto"
      );
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Administración"
          title={isEditing ? "Editar producto" : "Crear producto"}
          description="Panel simple para gestionar el catálogo desde la interfaz web."
        />

        <form onSubmit={handleSubmit} className="mt-6 grid gap-3 md:grid-cols-2">
          <input
            required
            placeholder="Nombre"
            value={form.name}
            onChange={(event) =>
              setForm((current) => ({ ...current, name: event.target.value }))
            }
            className="rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />
          <input
            required
            type="number"
            step="0.01"
            min="0.01"
            placeholder="Precio"
            value={form.price}
            onChange={(event) =>
              setForm((current) => ({ ...current, price: event.target.value }))
            }
            className="rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />
          <input
            required
            type="number"
            min="0"
            placeholder="Stock"
            value={form.stock}
            onChange={(event) =>
              setForm((current) => ({ ...current, stock: event.target.value }))
            }
            className="rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />
          <input
            placeholder="Categoría"
            value={form.category}
            onChange={(event) =>
              setForm((current) => ({ ...current, category: event.target.value }))
            }
            className="rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />
          <input
            placeholder="URL de imagen"
            value={form.imageUrl}
            onChange={(event) =>
              setForm((current) => ({ ...current, imageUrl: event.target.value }))
            }
            className="md:col-span-2 rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />
          <textarea
            placeholder="Descripción"
            value={form.description}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                description: event.target.value,
              }))
            }
            rows={3}
            className="md:col-span-2 rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
          />

          <label className="inline-flex items-center gap-2 text-sm font-semibold text-ink/80">
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
            Producto activo
          </label>

          <div className="flex items-center gap-2 md:justify-end">
            {isEditing && (
              <button
                type="button"
                onClick={resetForm}
                className="rounded-xl border border-ink/20 px-4 py-2.5 text-sm font-semibold"
              >
                Cancelar edición
              </button>
            )}
            <button
              type="submit"
              disabled={saving}
              className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-cream disabled:opacity-60"
            >
              {saving
                ? "Guardando..."
                : isEditing
                  ? "Actualizar"
                  : "Crear producto"}
            </button>
          </div>
        </form>

        {message && (
          <div className="mt-4 rounded-xl bg-sand px-4 py-3 text-sm text-ink/80">
            {message}
          </div>
        )}
      </section>

      <section className="rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Catálogo"
          title="Productos actuales"
          description="Edita o elimina productos existentes."
        />

        {loading ? (
          <p className="mt-4 text-sm text-ink/70">Cargando productos...</p>
        ) : (
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {products.map((product) => (
              <article
                key={product.id}
                className="rounded-2xl border border-ink/10 bg-white p-4"
              >
                <h3 className="font-semibold text-ink">{product.name}</h3>
                <p className="text-sm text-ink/65 line-clamp-2">
                  {product.description}
                </p>
                <p className="mt-2 text-sm font-semibold text-coral">
                  {formatCurrency(product.price)} - Stock: {product.stock}
                </p>
                <p className="text-xs text-ink/60">Estado: {product.active ? "Activo" : "Inactivo"}</p>

                <div className="mt-3 flex gap-2">
                  <button
                    onClick={() => handleEdit(product)}
                    className="rounded-xl border border-ink/20 px-3 py-2 text-xs font-semibold text-ink"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(product.id)}
                    className="rounded-xl border border-coral/30 px-3 py-2 text-xs font-semibold text-coral"
                  >
                    Eliminar
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
