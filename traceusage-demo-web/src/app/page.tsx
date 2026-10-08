"use client";

import type { Product } from "@/types/product";
import { useEffect, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_DEMO_API_URL ?? "http://localhost:8081";

export default function HomePage() {
  const [product, setProduct] = useState<Product | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadProduct() {
      try {
        const response = await fetch(`${API_URL}/api/products/101`);
        if (!response.ok) {
          throw new Error(`Demo API returned HTTP ${response.status}`);
        }

        const data = (await response.json()) as Product;
        setProduct(data);
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : "Failed to load product");
      } finally {
        setLoading(false);
      }
    }

    loadProduct();
  }, []);

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 p-6 text-slate-900">
      <section className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-900/10">
        <p className="text-sm font-medium text-slate-500">TraceUsage V2 controlled frontend demo</p>
        {loading && <p className="mt-6 text-slate-600">Loading product...</p>}
        {error && <p className="mt-6 text-red-600">{error}</p>}
        {product && (
          <>
            <h2 className="mt-6 text-3xl font-bold tracking-tight">{product.name}</h2>
            <p className="mt-3 text-2xl font-extrabold text-blue-600">Rs. {product.price}</p>
            <p className="mt-6 text-slate-500">
              The API response includes more fields, but this UI intentionally reads only name and price.
            </p>
          </>
        )}
      </section>
    </main>
  );
}
