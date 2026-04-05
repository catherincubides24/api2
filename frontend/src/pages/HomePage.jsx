import { useEffect, useMemo, useState } from "react";
import ProductCard from "../components/ProductCard";
import SectionTitle from "../components/SectionTitle";
import { useCart } from "../context/CartContext";
import { productService } from "../services/productService";

export default function HomePage() {
  const { addToCart } = useCart();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Todas");
  const [flash, setFlash] = useState("");

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError("");
        const data = await productService.getPublicProducts();
        setProducts(data);
      } catch (err) {
        setError(
          err.response?.data?.message ||
            "No pudimos cargar los productos por ahora."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const categories = useMemo(() => {
    const set = new Set(
      products.map((product) => product.category).filter(Boolean)
    );
    return ["Todas", ...Array.from(set)];
  }, [products]);

  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const bySearch =
        product.name.toLowerCase().includes(search.toLowerCase()) ||
        (product.description || "")
          .toLowerCase()
          .includes(search.toLowerCase());
      const byCategory =
        selectedCategory === "Todas" || product.category === selectedCategory;

      return bySearch && byCategory;
    });
  }, [products, search, selectedCategory]);

  const handleAddToCart = (product) => {
    addToCart(product);
    setFlash(`${product.name} agregado al carrito`);
    setTimeout(() => setFlash(""), 1800);
  };

  return (
    <div className="space-y-10">
      <section className="relative overflow-hidden rounded-[2rem] border border-white/70 bg-gradient-to-br from-ink via-dusk to-mint px-6 py-12 text-cream shadow-card sm:px-10">
        <div className="absolute -right-10 top-6 h-36 w-36 animate-float rounded-full bg-coral/40 blur-2xl" />
        <div className="absolute -bottom-8 left-20 h-28 w-28 rounded-full bg-peach/30 blur-2xl" />

        <div className="relative grid items-center gap-8 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="space-y-5">
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-peach">
              Tienda online para mascotas
            </p>
            <h1 className="font-display text-4xl leading-tight sm:text-5xl">
              Todo para tu mascota en un solo lugar.
            </h1>
            <p className="max-w-xl text-cream/85">
              Descubre alimentos, accesorios y productos de cuidado diario con
              precios claros, compra segura y seguimiento de pedidos en tiempo
              real.
            </p>
            <a
              href="#catalogo"
              className="inline-flex rounded-full bg-coral px-6 py-3 text-sm font-semibold text-white transition hover:bg-coral/90"
            >
              Explorar catálogo
            </a>
          </div>

          <div className="rounded-3xl border border-white/15 bg-white/10 p-6 backdrop-blur">
            <p className="text-sm text-cream/90">¿Por qué comprar aquí?</p>
            <ul className="mt-3 space-y-2 text-sm text-cream/80">
              <li>Catálogo actualizado para perros, gatos y más</li>
              <li>Compra en minutos con carrito intuitivo</li>
              <li>Seguimiento del estado de tu pedido</li>
              <li>Atención enfocada en bienestar animal</li>
            </ul>
          </div>
        </div>
      </section>

      <section id="catalogo" className="space-y-6">
        <SectionTitle
          eyebrow="Catálogo"
          title="Productos para tu mascota"
          description="Filtra por categoría, busca por nombre y arma tu carrito en segundos."
        />

        <div className="grid gap-3 rounded-2xl bg-white/80 p-4 shadow-soft md:grid-cols-[1fr_auto] md:items-center">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar productos..."
            className="w-full rounded-xl border border-ink/15 bg-white px-4 py-2 outline-none ring-coral/30 transition focus:ring"
          />

          <div className="flex flex-wrap gap-2">
            {categories.map((category) => (
              <button
                key={category}
                onClick={() => setSelectedCategory(category)}
                className={[
                  "rounded-full px-4 py-2 text-xs font-semibold transition",
                  selectedCategory === category
                    ? "bg-ink text-cream"
                    : "bg-sand text-ink hover:bg-peach",
                ].join(" ")}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {flash && (
          <div className="rounded-2xl border border-mint/30 bg-mint/15 px-4 py-3 text-sm text-dusk">
            {flash}
          </div>
        )}

        {loading && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, index) => (
              <div
                key={index}
                className="h-80 animate-pulse rounded-3xl bg-white/75"
              />
            ))}
          </div>
        )}

        {error && (
          <div className="rounded-2xl border border-coral/30 bg-coral/10 px-4 py-3 text-sm text-coral">
            {error}
          </div>
        )}

        {!loading && !error && filteredProducts.length === 0 && (
          <div className="rounded-2xl bg-white/80 px-4 py-8 text-center text-ink/70 shadow-soft">
            No encontramos productos con esos filtros.
          </div>
        )}

        <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
          {filteredProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              onAddToCart={handleAddToCart}
            />
          ))}
        </div>
      </section>
    </div>
  );
}
