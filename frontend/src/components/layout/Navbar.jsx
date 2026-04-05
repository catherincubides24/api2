import { Menu, PawPrint, ReceiptText, ShoppingBag, UserCircle, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";

function navClass({ isActive }) {
  return [
    "rounded-full px-4 py-2 text-sm font-semibold transition",
    isActive
      ? "bg-ink text-cream"
      : "text-ink/80 hover:bg-white/70 hover:text-ink",
  ].join(" ");
}

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { itemCount } = useCart();
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 border-b border-ink/5 bg-cream/80 backdrop-blur-md">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2 text-ink">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-ink text-cream shadow-soft">
            <PawPrint size={20} />
          </span>
          <div>
            <p className="font-display text-2xl leading-none">Huellitas Shop</p>
            <p className="text-xs text-ink/60">Cuidado y estilo para tu mascota</p>
          </div>
        </Link>

        <nav className="hidden items-center gap-2 md:flex">
          <NavLink to="/" className={navClass}>
            Productos
          </NavLink>
          <NavLink to="/cart" className={navClass}>
            <span className="inline-flex items-center gap-2">
              <ShoppingBag size={16} />
              Carrito
              {itemCount > 0 && (
                <span className="rounded-full bg-coral px-2 py-0.5 text-xs text-white">
                  {itemCount}
                </span>
              )}
            </span>
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/purchases" className={navClass}>
              <span className="inline-flex items-center gap-2">
                <ReceiptText size={16} />
                Mis compras
              </span>
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/admin" className={navClass}>
              Admin
            </NavLink>
          )}
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          {isAuthenticated ? (
            <>
              <div className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm shadow-soft">
                <UserCircle size={18} className="text-mint" />
                <span>{user?.fullName}</span>
              </div>
              <button
                onClick={logout}
                className="rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream transition hover:bg-dusk"
              >
                Cerrar sesión
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink transition hover:bg-white"
              >
                Ingresar
              </Link>
              <Link
                to="/register"
                className="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-coral/90"
              >
                Crear cuenta
              </Link>
            </>
          )}
        </div>

        <button
          onClick={() => setOpen((prev) => !prev)}
          className="inline-flex rounded-full border border-ink/20 p-2 text-ink md:hidden"
          aria-label="Abrir menú"
        >
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {open && (
        <div className="border-t border-ink/10 bg-cream px-4 py-4 md:hidden">
          <div className="flex flex-col gap-2">
            <NavLink to="/" onClick={() => setOpen(false)} className={navClass}>
              Productos
            </NavLink>
            <NavLink to="/cart" onClick={() => setOpen(false)} className={navClass}>
              Carrito ({itemCount})
            </NavLink>
            {isAuthenticated && (
              <NavLink
                to="/purchases"
                onClick={() => setOpen(false)}
                className={navClass}
              >
                Mis compras
              </NavLink>
            )}
            {isAdmin && (
              <NavLink to="/admin" onClick={() => setOpen(false)} className={navClass}>
                Admin
              </NavLink>
            )}
            {isAuthenticated ? (
              <button
                onClick={() => {
                  logout();
                  setOpen(false);
                }}
                className="rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream"
              >
                Cerrar sesión
              </button>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setOpen(false)}
                  className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink"
                >
                  Ingresar
                </Link>
                <Link
                  to="/register"
                  onClick={() => setOpen(false)}
                  className="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white"
                >
                  Crear cuenta
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
