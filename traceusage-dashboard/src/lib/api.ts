import { getToken } from "./auth";

const API_URL = process.env.NEXT_PUBLIC_TRACEUSAGE_API_URL ?? "http://localhost:8080";

type ApiFetchOptions = RequestInit & {
  auth?: boolean;
};

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const { auth = true, headers, ...requestOptions } = options;
  const token = auth ? getToken() : null;

  const response = await fetch(`${API_URL}${path}`, {
    ...requestOptions,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(body?.message ?? "Request failed");
  }

  return body as T;
}
