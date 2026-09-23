const FIVE_MINUTES = 5 * 60 * 1000;
const TWO_AND_HALF_MINUTES = 2.5 * 60 * 1000;

function toPositiveMs(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const timeoutMs = toPositiveMs(
  import.meta.env.VITE_INACTIVITY_TIMEOUT_MS,
  FIVE_MINUTES
);
const warningMs = toPositiveMs(
  import.meta.env.VITE_INACTIVITY_WARNING_MS,
  TWO_AND_HALF_MINUTES
);

export const inactivityConfig = {
  timeoutMs,
  // El aviso nunca puede llegar después del cierre.
  warningMs: warningMs < timeoutMs ? warningMs : timeoutMs / 2,
};