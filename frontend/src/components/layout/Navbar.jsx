import { Database, Menu, PawPrint, ReceiptText, ShoppingBag, UserCircle, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import { useLanguage } from "../../context/LanguageContext";
import LanguageToggle from "../LanguageToggle";
import ThemeToggle from "../ThemeToggle";

function navClass({ isActive }) {
  return [
    "rounded-full px-4 py-2 text-sm font-semibold transition",
    isActive
      ? "bg-ink text-cream"
      : "text-ink/80 hover:bg-white/70 hover:text-ink",
  ].join(" ");
}

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, isStaff, logout } = useAuth();
  const { itemCount } = useCart();
  const { t } = useLanguage();
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 border-b border-ink/5 bg-cream/80 backdrop-blur-md dark:border-white/10 dark:bg-slate-900/80">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2 text-ink dark:text-cream">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-ink text-cream shadow-soft">
            <PawPrint size={20} />
          </span>
          <div>
            <p className="font-display text-2xl leading-none">Huellitas Shop</p>
            <p className="text-xs text-ink/60 dark:text-cream/60">{t("navbar.subtitle")}</p>
          </div>
        </Link>

        {/* ───────── Menú de escritorio ───────── */}
        <nav className="hidden items-center gap-2 md:flex">
          {isStaff ? (
            <>
              <NavLink to="/admin" className={navClass} end>
                {t("navbar.products")}
              </NavLink>
              <NavLink to="/admin/sales" className={navClass}>
                {t("navbar.sales")}
              </NavLink>
              <NavLink to="/admin/orders" className={navClass}>
                {t("navbar.orders")}
              </NavLink>
              <NavLink to="/admin/reports" className={navClass}>
                {t("navbar.reports")}
              </NavLink>
              <NavLink to="/backup" className={navClass}>
                <span className="inline-flex items-center gap-1.5">
                  <Database size={16} />
                  {t("navbar.backup")}
                </span>
              </NavLink>
              {isAdmin && (
                <NavLink to="/admin/logs" className={navClass}>
                  {t("navbar.logs")}
                </NavLink>
              )}
            </>
          ) : (
            <>
              <NavLink to="/" className={navClass}>
                {t("navbar.products")}
              </NavLink>
              <NavLink to="/cart" className={navClass}>
                <span className="inline-flex items-center gap-2">
                  <ShoppingBag size={16} />
                  {t("navbar.cart")}
                  {itemCount > 0 && (
                    <span className="rounded-full bg-coral px-2 py-0.5 text-xs text-white">
                      {itemCount}
                    </span>
                  )}
                </span>
              </NavLink>
              {isAuthenticated && (
                <>
                  <NavLink to="/purchases" className={navClass}>
                    <span className="inline-flex items-center gap-2">
                      <ReceiptText size={16} />
                      {t("navbar.myPurchases")}
                    </span>
                  </NavLink>
                </>
              )}
            </>
          )}
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          <ThemeToggle />
          <LanguageToggle />
          {isAuthenticated ? (
            <>
              <div className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm shadow-soft dark:bg-slate-800 dark:text-cream">
                <UserCircle size={18} className="text-mint" />
                <span>{user?.fullName}</span>
              </div>
              <button
                onClick={logout}
                className="rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream transition hover:bg-dusk"
              >
                {t("navbar.logout")}
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink transition hover:bg-white dark:text-cream dark:hover:bg-slate-800"
              >
                {t("navbar.login")}
              </Link>
              <Link
                to="/register"
                className="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-coral/90"
              >
                {t("navbar.register")}
              </Link>
            </>
          )}
        </div>

        <button
          onClick={() => setOpen((prev) => !prev)}
          className="inline-flex rounded-full border border-ink/20 p-2 text-ink md:hidden dark:text-cream"
          aria-label={t("navbar.openMenu")}
        >
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* ───────── Menú móvil ───────── */}
      {open && (
        <div className="border-t border-ink/10 bg-cream px-4 py-4 md:hidden dark:border-white/10 dark:bg-slate-900">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2 pb-2">
              <ThemeToggle />
              <LanguageToggle />
            </div>
            {isStaff ? (
              <>
                <NavLink to="/admin" onClick={() => setOpen(false)} className={navClass} end>
                  {t("navbar.products")}
                </NavLink>
                <NavLink to="/admin/sales" onClick={() => setOpen(false)} className={navClass}>
                  {t("navbar.sales")}
                </NavLink>
                <NavLink to="/admin/orders" onClick={() => setOpen(false)} className={navClass}>
                  {t("navbar.orders")}
                </NavLink>
                <NavLink to="/admin/reports" onClick={() => setOpen(false)} className={navClass}>
                  {t("navbar.reports")}
                </NavLink>
                <NavLink to="/backup" onClick={() => setOpen(false)} className={navClass}>
                  <span className="inline-flex items-center gap-1.5">
                    <Database size={16} />
                    {t("navbar.backup")}
                  </span>
                </NavLink>
                {isAdmin && (
                  <NavLink to="/admin/logs" onClick={() => setOpen(false)} className={navClass}>
                    {t("navbar.logs")}
                  </NavLink>
                )}
              </>
            ) : (
              <>
                <NavLink to="/" onClick={() => setOpen(false)} className={navClass}>
                  {t("navbar.products")}
                </NavLink>
                <NavLink to="/cart" onClick={() => setOpen(false)} className={navClass}>
                  {t("navbar.cart")} ({itemCount})
                </NavLink>
                {isAuthenticated && (
                  <>
                    <NavLink
                      to="/purchases"
                      onClick={() => setOpen(false)}
                      className={navClass}
                    >
                      {t("navbar.myPurchases")}
                    </NavLink>
                  </>
                )}
              </>
            )}
            {isAuthenticated ? (
              <button
                onClick={() => {
                  logout();
                  setOpen(false);
                }}
                className="rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream"
              >
                {t("navbar.logout")}
              </button>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setOpen(false)}
                  className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink"
                >
                  {t("navbar.login")}
                </Link>
                <Link
                  to="/register"
                  onClick={() => setOpen(false)}
                  className="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white"
                >
                  {t("navbar.register")}
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}