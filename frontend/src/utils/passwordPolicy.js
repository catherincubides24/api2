export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 25;

// Misma definición que PasswordRule.java
export const PASSWORD_RULES = [
  {
    id: "length",
    labelKey: "password.rule.length",
    test: (p) => p.length >= PASSWORD_MIN_LENGTH && p.length <= PASSWORD_MAX_LENGTH,
  },
  { id: "upper", labelKey: "password.rule.upper", test: (p) => /\p{Lu}/u.test(p) },
  { id: "lower", labelKey: "password.rule.lower", test: (p) => /\p{Ll}/u.test(p) },
  { id: "digit", labelKey: "password.rule.digit", test: (p) => /\p{Nd}/u.test(p) },
  { id: "special", labelKey: "password.rule.special", test: (p) => /[^\p{L}\p{Nd}\s]/u.test(p) },
];

export function evaluatePassword(password) {
  return PASSWORD_RULES.map((rule) => ({
    id: rule.id,
    labelKey: rule.labelKey,
    met: rule.test(password),
  }));
}

export function isPasswordValid(password) {
  return PASSWORD_RULES.every((rule) => rule.test(password));
}