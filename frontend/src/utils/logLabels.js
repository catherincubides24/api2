export const actionLabels = {
  CREATE: "Creación",
  UPDATE: "Modificación",
  DELETE: "Eliminación",
  LOGIN_SUCCESS: "Inicio de sesión",
  LOGIN_FAILURE: "Login fallido",
  LOGOUT: "Cierre de sesión",
};

export const actionStyles = {
  CREATE: "bg-emerald-100 text-emerald-700",
  UPDATE: "bg-sky-100 text-sky-700",
  DELETE: "bg-rose-100 text-rose-700",
  LOGIN_SUCCESS: "bg-slate-100 text-slate-700",
  LOGIN_FAILURE: "bg-amber-100 text-amber-700",
  LOGOUT: "bg-slate-100 text-slate-700",
};

const entityLabels = {
  Product: "producto",
  User: "usuario",
  PetOrder: "pedido",
  OrderItem: "ítem de pedido",
};

export const fieldLabels = {
  fullName: "Nombre",
  email: "Email",
  role: "Rol",
  name: "Nombre",
  description: "Descripción",
  price: "Precio",
  stock: "Stock",
  imageUrl: "Imagen",
  category: "Categoría",
  active: "Activo",
  status: "Estado",
  paymentMethod: "Método de pago",
  ticketNumber: "Ticket",
  totalAmount: "Total",
  user: "Cliente",
  order: "Pedido",
  product: "Producto",
  quantity: "Cantidad",
  unitPrice: "Precio unitario",
  createdAt: "Creado",
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

export function fieldLabel(field) {
  return fieldLabels[field] || field;
}

export function formatValue(value) {
  if (value === true) return "Sí";
  if (value === false) return "No";
  return value === null || value === undefined || value === "" ? "—" : String(value);
}

function targetOf(log, parsed) {
  const key = parsed && NAME_KEYS.find((k) => parsed[k]);
  return key ? `«${parsed[key]}» (#${log.entityId})` : `#${log.entityId}`;
}

/** Frase corta que explica qué pasó exactamente. */
export function describeLog(log) {
  const parsed = parseDetails(log.details);
  const entity = entityLabels[log.entityName] || log.entityName;

  switch (log.action) {
    case "LOGIN_SUCCESS":
      return "Inició sesión";
    case "LOGIN_FAILURE":
      return "Intento de inicio de sesión fallido";
    case "LOGOUT":
      return "Cerró sesión";
    case "CREATE":
      return `Creó ${entity} ${targetOf(log, parsed)}`;
    case "DELETE":
      return `Eliminó ${entity} ${targetOf(log, parsed)}`;
    case "UPDATE": {
      const fields = parsed ? Object.keys(parsed).map(fieldLabel).join(", ") : "";
      return `Modificó ${entity} #${log.entityId}${fields ? `: ${fields}` : ""}`;
    }
    default:
      return log.action;
  }
}