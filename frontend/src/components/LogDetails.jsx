import { fieldLabel, formatValue, parseDetails } from "../utils/logLabels";

function formatEntry([field, value]) {
  const isDiff = value && typeof value === "object" && "antes" in value;
  return isDiff
    ? `${fieldLabel(field)}: ${formatValue(value.antes)} → ${formatValue(value.despues)}`
    : `${fieldLabel(field)}: ${formatValue(value)}`;
}

/** Muestra el detalle de un log de forma legible y colapsable. */
export default function LogDetails({ details }) {
  if (!details) return <span className="text-ink/40">—</span>;

  const parsed = parseDetails(details);
  const lines = parsed ? Object.entries(parsed).map(formatEntry) : [details];

  return (
    <details className="text-xs text-ink/80">
      <summary className="cursor-pointer font-semibold text-ink">Ver detalle</summary>
      <ul className="mt-1 max-h-40 space-y-0.5 overflow-y-auto rounded-lg bg-sand/70 p-2">
        {lines.map((line, index) => (
          <li key={index} className="break-all">{line}</li>
        ))}
      </ul>
    </details>
  );
}