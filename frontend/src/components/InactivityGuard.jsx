import { useNavigate } from "react-router-dom";
import { inactivityConfig } from "../config/inactivity";
import { useAuth } from "../context/AuthContext";
import { useIdleTimer } from "../hooks/useIdleTimer";
import SessionTimeoutModal from "./SessionTimeoutModal";

export default function InactivityGuard() {
  const { isAuthenticated, user, login, logout } = useAuth();
  const navigate = useNavigate();

  const closeSession = () => {
    logout();
    navigate("/login", { replace: true, state: { reason: "inactivity" } });
  };

  const { isWarning, remainingMs, reset } = useIdleTimer({
    enabled: isAuthenticated,
    warningAfterMs: inactivityConfig.warningMs,
    timeoutAfterMs: inactivityConfig.timeoutMs,
    onTimeout: closeSession,
  });

  // Reautenticación: reutiliza el login existente (valida y renueva el token).
  const confirmPassword = async (password) => {
    await login({ email: user.email, password });
    reset();
  };

  if (!isAuthenticated || !isWarning) return null;

  return (
    <SessionTimeoutModal
      remainingMs={remainingMs}
      onConfirm={confirmPassword}
      onLogout={closeSession}
    />
  );
}