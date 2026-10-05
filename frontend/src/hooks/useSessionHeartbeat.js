import { useEffect } from "react";
import { authService } from "../services/authService";

const HEARTBEAT_INTERVAL_MS = 30000;

/** Avisa al backend que esta sesión sigue abierta mientras haya un usuario autenticado. */
export function useSessionHeartbeat(enabled) {
  useEffect(() => {
    if (!enabled) return undefined;

    const id = setInterval(() => {
      authService.heartbeat().catch(() => {});
    }, HEARTBEAT_INTERVAL_MS);

    return () => clearInterval(id);
  }, [enabled]);
}