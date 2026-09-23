import { X } from "lucide-react";
import { privacyPolicySections, PRIVACY_POLICY_UPDATED_AT } from "../data/privacyPolicyContent";

export default function PrivacyPolicyModal({ open, onClose }) {
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/50 p-4"
      onClick={onClose}
    >
      <div
        className="max-h-[80vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-6 shadow-card"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-2xl text-ink">
            Política de Tratamiento de Datos Personales
          </h2>
          <button
            onClick={onClose}
            className="rounded-full p-2 text-ink/60 hover:bg-sand"
            aria-label="Cerrar"
          >
            <X size={20} />
          </button>
        </div>

        <p className="mb-4 text-xs text-ink/50">
          Última actualización: {PRIVACY_POLICY_UPDATED_AT}
        </p>

        <div className="space-y-4">
          {privacyPolicySections.map((section) => (
            <div key={section.title}>
              <h3 className="font-semibold text-ink">{section.title}</h3>
              <p className="mt-1 text-sm text-ink/70">{section.body}</p>
            </div>
          ))}
        </div>

        <button
          onClick={onClose}
          className="mt-6 w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream"
        >
          Cerrar
        </button>
      </div>
    </div>
  );
}