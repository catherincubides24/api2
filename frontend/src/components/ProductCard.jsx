import { ShoppingCart } from "lucide-react";
import { useLanguage } from "../context/LanguageContext";
import { formatCurrency } from "../utils/formatCurrency";

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1517849845537-4d257902454a?auto=format&fit=crop&w=800&q=80";

export default function ProductCard({ product, onAddToCart }) {
  const { t, tCategory, tProductName, tProductDesc } = useLanguage();

  return (
    <article className="group animate-rise overflow-hidden rounded-3xl border border-white/70 bg-white/90 shadow-card transition hover:-translate-y-1 dark:border-white/10 dark:bg-slate-800/90">
      <div className="relative h-52 overflow-hidden">
        <img
          src={product.imageUrl || FALLBACK_IMAGE}
          alt={tProductName(product.name)}
          className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
          loading="lazy"
        />
        <span className="absolute left-3 top-3 rounded-full bg-ink/85 px-3 py-1 text-xs font-semibold text-cream">
          {tCategory(product.category) || t("product.general")}
        </span>
      </div>

      <div className="space-y-4 p-5">
        <div>
          <h3 className="text-lg font-bold text-ink dark:text-cream">{tProductName(product.name)}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-ink/65 dark:text-cream/65">
            {tProductDesc(product.name, product.description)}
          </p>
        </div>

        <div className="flex items-end justify-between">
          <div>
            <p className="text-2xl font-extrabold text-coral">
              {formatCurrency(product.price)}
            </p>
            <p className="text-xs text-ink/60 dark:text-cream/60">
              {t("product.stock", { count: product.stock })}
            </p>
          </div>

          <button
            onClick={() => onAddToCart(product)}
            className="inline-flex items-center gap-2 rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream transition hover:bg-dusk disabled:cursor-not-allowed disabled:opacity-50 dark:bg-cream dark:text-ink dark:hover:bg-white"
            disabled={!product.active || product.stock <= 0}
          >
            <ShoppingCart size={16} />
            {t("product.add")}
          </button>
        </div>
      </div>
    </article>
  );
}