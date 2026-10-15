import { useLanguage } from "../context/LanguageContext";
import { fieldLabel, formatValue, parseDetails } from "../utils/logLabels";

function formatEntry([field, value], t) {
  const isDiff = value && typeof value === "object" && "antes" in value;
  return isDiff
    ? `${fieldLabel(field, t)}: ${formatValue(value.antes, t)} → ${formatValue(value.despues, t)}`
    : `${fieldLabel(field, t)}: ${formatValue(value, t)}`;
}

/** Muestra el detalle de un log de forma legible y colapsable. */
export default function LogDetails({ details }) {
  const { t } = useLanguage();

  if (!details) return <span className="text-ink/40">—</span>;

  const parsed = parseDetails(details);
  const lines = parsed ? Object.entries(parsed).map((e) => formatEntry(e, t)) : [details];

  return (
    <details className="text-xs text-ink/80">
      <summary className="cursor-pointer font-semibold text-ink">{t("logLabels.viewDetail")}</summary>
      <ul className="mt-1 max-h-40 space-y-0.5 overflow-y-auto rounded-lg bg-sand/70 p-2">
        {lines.map((line, index) => (
          <li key={index} className="break-all">{line}</li>
        ))}
      </ul>
    </details>
  );
}