import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useAuth } from "./AuthContext";

const CartContext = createContext(null);
const CART_PREFIX = "petshop_cart_";
const GUEST_CART_KEY = `${CART_PREFIX}guest`;

function getCartKey(userId) {
  return userId ? `${CART_PREFIX}${userId}` : GUEST_CART_KEY;
}

function readCart(key) {
  const raw = localStorage.getItem(key);
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(key);
    return [];
  }
}

export function CartProvider({ children }) {
  const { user } = useAuth();
  const cartKey = getCartKey(user?.id);
  const [items, setItems] = useState(() => readCart(cartKey));

  // Cada vez que cambia el usuario (login, logout o cambio de cuenta),
  // se recarga SOLO el carrito que le pertenece a ese usuario.
  useEffect(() => {
    setItems(readCart(cartKey));
  }, [cartKey]);

  useEffect(() => {
    localStorage.setItem(cartKey, JSON.stringify(items));
  }, [items, cartKey]);

  const addToCart = (product, quantity = 1) => {
    setItems((current) => {
      const existing = current.find((item) => item.id === product.id);
      if (existing) {
        return current.map((item) =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + quantity }
            : item
        );
      }
      return [...current, { ...product, quantity }];
    });
  };

  const removeFromCart = (id) => {
    setItems((current) => current.filter((item) => item.id !== id));
  };

  const updateQuantity = (id, quantity) => {
    setItems((current) =>
      current
        .map((item) => (item.id === id ? { ...item, quantity } : item))
        .filter((item) => item.quantity > 0)
    );
  };

  const clearCart = () => setItems([]);

  const itemCount = useMemo(
    () => items.reduce((acc, item) => acc + item.quantity, 0),
    [items]
  );

  const total = useMemo(
    () =>
      items.reduce(
        (acc, item) => acc + Number(item.price || 0) * item.quantity,
        0
      ),
    [items]
  );

  const value = useMemo(
    () => ({
      items,
      itemCount,
      total,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
    }),
    [items, itemCount, total]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart debe usarse dentro de CartProvider");
  }
  return context;
}