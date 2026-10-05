import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useSessionHeartbeat } from "../hooks/useSessionHeartbeat";
import { onSessionInvalid } from "../services/api";
import { authService } from "../services/authService";

const AuthContext = createContext(null);

const TOKEN_KEY = "petshop_token";
const USER_KEY = "petshop_user";
const SESSION_REPLACED_MESSAGE =
  "Tu sesión se cerró porque se inició sesión en otro navegador.";

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
  const [sessionNotice, setSessionNotice] = useState("");

  const clearSession = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }, []);

  useEffect(() => {
    onSessionInvalid(() => {
      clearSession();
      setSessionNotice(SESSION_REPLACED_MESSAGE);
    });
    return () => onSessionInvalid(null);
  }, [clearSession]);

  const isAuthenticated = Boolean(token && user);
  useSessionHeartbeat(isAuthenticated);

  const persistSession = (session) => {
    const nextUser = {
      id: session.id,
      fullName: session.fullName,
      email: session.email,
      role: session.role,
    };

    setSessionNotice("");
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

  const logout = async () => {
    try {
      await authService.logout();
    } catch {
      // Si el servidor no responde igual se cierra la sesión local.
    } finally {
      clearSession();
    }
  };

  const dismissSessionNotice = () => setSessionNotice("");

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated,
      isAdmin: user?.role === "ADMIN",
      sessionNotice,
      dismissSessionNotice,
      login,
      register,
      logout,
    }),
    [token, user, sessionNotice]
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