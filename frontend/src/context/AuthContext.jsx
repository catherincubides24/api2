import { createContext, useContext, useMemo, useState } from "react";
import { authService } from "../services/authService";

const AuthContext = createContext(null);

const TOKEN_KEY = "petshop_token";
const USER_KEY = "petshop_user";

function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(getStoredUser);

  const persistSession = (session) => {
    const nextUser = {
      id: session.id,
      fullName: session.fullName,
      email: session.email,
      role: session.role,
    };
    setToken(session.token);
    setUser(nextUser);
    localStorage.setItem(TOKEN_KEY, session.token);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
  };

  const login = async (credentials) => {
    const session = await authService.login(credentials);
    persistSession(session);
    return session;
  };

  const register = async (payload) => {
    const session = await authService.register(payload);
    persistSession(session);
    return session;
  };

  // Al hacer logout SOLO se borra el token y el usuario.
  // CartContext detecta que user?.id cambió a undefined
  // y automáticamente vacía el carrito en memoria (sin borrar el
  // carrito guardado en localStorage del usuario anterior).
  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  };

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      isAdmin: user?.role === "ADMIN",
      login,
      register,
      logout,
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe usarse dentro de AuthProvider");
  }
  return context;
}