const ARRAY_INDEX_PATTERN = /^(0|[1-9]\d*)$/;

export function isArrayIndex(property: PropertyKey): boolean {
  return typeof property === "string" && ARRAY_INDEX_PATTERN.test(property);
}

export function toPropertyName(property: PropertyKey): string | null {
  if (typeof property === "symbol") {
    return null;
  }

  return String(property);
}

export function buildFieldPath(basePath: string, property: PropertyKey, parentIsArray: boolean): string | null {
  const propertyName = toPropertyName(property);
  if (propertyName === null) {
    return null;
  }

  if (parentIsArray && isArrayIndex(propertyName)) {
    return basePath ? `${basePath}[]` : "[]";
  }

  if (!basePath) {
    return propertyName;
  }

  return `${basePath}.${propertyName}`;
}

export function isIgnoredProperty(target: unknown, property: PropertyKey): boolean {
  if (typeof property === "symbol") {
    return true;
  }

  const propertyName = String(property);

  if (propertyName === "constructor" || propertyName === "prototype" || propertyName === "__proto__") {
    return true;
  }

  if (Array.isArray(target)) {
    return propertyName === "length" || propertyName in Array.prototype;
  }

  return propertyName === "toJSON";
}
