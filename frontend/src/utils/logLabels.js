export const actionStyles = {
  CREATE: "bg-emerald-100 text-emerald-700",
  UPDATE: "bg-sky-100 text-sky-700",
  DELETE: "bg-rose-100 text-rose-700",
  LOGIN_SUCCESS: "bg-slate-100 text-slate-700",
  LOGIN_FAILURE: "bg-amber-100 text-amber-700",
  LOGOUT: "bg-slate-100 text-slate-700",
};

const NAME_KEYS = ["name", "fullName", "ticketNumber", "product"];

export function parseDetails(details) {
  if (!details) return null;
  try {
    return JSON.parse(details);
  } catch {
    return null;
  }
}

/** Retorna la etiqueta de acción traducida usando t() del LanguageContext */
export function actionLabel(action, t) {
  const translated = t(`logLabels.actions.${action}`);
  // Si t() devuelve la clave (fallback cuando no existe), usa la acción cruda
  return translated !== `logLabels.actions.${action}` ? translated : action;
}

/** Retorna la etiqueta de un campo traducida */
export function fieldLabel(field, t) {
  const translated = t(`logLabels.fields.${field}`);
  return translated !== `logLabels.fields.${field}` ? translated : field;
}

/** Formatea un valor booleano/nulo con soporte i18n */
export function formatValue(value, t) {
  if (value === true) return t("logLabels.boolTrue");
  if (value === false) return t("logLabels.boolFalse");
  return value === null || value === undefined || value === "" ? "—" : String(value);
}

function targetOf(log, parsed) {
  const key = parsed && NAME_KEYS.find((k) => parsed[k]);
  return key ? `«${parsed[key]}» (#${log.entityId})` : `#${log.entityId}`;
}

/**
 * Aplica reemplazos de tokens {key} en un string de plantilla.
 * Se usa para las claves de `describe` que tienen {entity}, {target}, {id}, {fields}.
 */
function applyReplacements(template, replacements) {
  return Object.entries(replacements).reduce(
    (str, [token, value]) => str.replace(new RegExp(`\\{${token}\\}`, "g"), value),
    template
  );
}

/** Frase corta que explica qué pasó exactamente, usando t() para i18n. */
export function describeLog(log, t) {
  const parsed = parseDetails(log.details);
  const entityKey = t(`logLabels.entities.${log.entityName}`);
  const entity =
    entityKey !== `logLabels.entities.${log.entityName}` ? entityKey : log.entityName;

  switch (log.action) {
    case "LOGIN_SUCCESS":
      return t("logLabels.describe.loginSuccess");
    case "LOGIN_FAILURE":
      return t("logLabels.describe.loginFailure");
    case "LOGOUT":
      return t("logLabels.describe.logout");
    case "CREATE":
      return applyReplacements(t("logLabels.describe.created"), {
        entity,
        target: targetOf(log, parsed),
      });
    case "DELETE":
      return applyReplacements(t("logLabels.describe.deleted"), {
        entity,
        target: targetOf(log, parsed),
      });
    case "UPDATE": {
      const fields = parsed ? Object.keys(parsed).map((f) => fieldLabel(f, t)).join(", ") : "";
      const templateKey = fields ? "logLabels.describe.updatedFields" : "logLabels.describe.updated";
      return applyReplacements(t(templateKey), {
        entity,
        id: log.entityId,
        fields,
      });
    }
    default:
      return log.action;
  }
}