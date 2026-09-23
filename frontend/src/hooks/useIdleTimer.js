import { useCallback, useEffect, useRef, useState } from "react";

const ACTIVITY_EVENTS = ["mousemove", "mousedown", "keydown", "scroll", "touchstart"];
const TICK_MS = 1000;

/**
 * Detecta inactividad del usuario.
 * - Pasado `warningAfterMs` entra en modo aviso (isWarning = true).
 * - En modo aviso la actividad normal se ignora: solo reset() lo cancela.
 * - Pasado `timeoutAfterMs` ejecuta onTimeout.
 */
export function useIdleTimer({ enabled, warningAfterMs, timeoutAfterMs, onTimeout }) {
  const [isWarning, setIsWarning] = useState(false);
  const [remainingMs, setRemainingMs] = useState(timeoutAfterMs - warningAfterMs);

  const lastActivityRef = useRef(Date.now());
  const isWarningRef = useRef(false);
  const onTimeoutRef = useRef(onTimeout);

  useEffect(() => {
    onTimeoutRef.current = onTimeout;
  });

  const reset = useCallback(() => {
    lastActivityRef.current = Date.now();
    isWarningRef.current = false;
    setIsWarning(false);
  }, []);

  useEffect(() => {
    if (!enabled) return undefined;

    reset();

    const handleActivity = () => {
      if (!isWarningRef.current) {
        lastActivityRef.current = Date.now();
      }
    };

    ACTIVITY_EVENTS.forEach((event) =>
      window.addEventListener(event, handleActivity, { passive: true })
    );

    const interval = setInterval(() => {
      const idleMs = Date.now() - lastActivityRef.current;

      if (idleMs >= timeoutAfterMs) {
        clearInterval(interval);
        reset();
        onTimeoutRef.current();
        return;
      }

      if (idleMs >= warningAfterMs) {
        isWarningRef.current = true;
        setIsWarning(true);
        setRemainingMs(timeoutAfterMs - idleMs);
      }
    }, TICK_MS);

    return () => {
      clearInterval(interval);
      ACTIVITY_EVENTS.forEach((event) =>
        window.removeEventListener(event, handleActivity)
      );
    };
  }, [enabled, warningAfterMs, timeoutAfterMs, reset]);

  return { isWarning, remainingMs, reset };
}