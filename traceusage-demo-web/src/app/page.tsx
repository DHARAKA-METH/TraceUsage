"use client";

import { getTraceUsageClient } from "@/lib/traceUsage";
import type { ApiError, Category, Order, Product, ProductSummary, ReviewsResponse } from "@/types/demo";
import { useCallback, useEffect, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_DEMO_API_URL ?? "http://localhost:8081";
type View = "catalog" | "detail" | "reviews" | "orders" | "tests";

async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { method, headers: body ? { "Content-Type": "application/json" } : undefined, body: body ? JSON.stringify(body) : undefined });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) throw new Error((data as ApiError)?.message ?? `Demo API returned HTTP ${response.status}`);
  return data as T;
}

function track<T>(data: T, method: string, endpoint: string, schema: string): T {
  return getTraceUsageClient().trackResponse(data, { method, endpoint, schema });
}

export default function HomePage() {
  const [view, setView] = useState<View>("catalog");
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<ReviewsResponse | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [message, setMessage] = useState("Loading catalog…");

  const loadCatalog = useCallback(async () => {
    try {
      const [catalog, categoryList] = await Promise.all([request<ProductSummary[]>("/api/products"), request<Category[]>("/api/categories")]);
      setProducts(track(catalog, "GET", "/api/products", "ProductList"));
      setCategories(track(categoryList, "GET", "/api/categories", "CategoryList"));
      setMessage("");
    } catch (error) { setMessage(error instanceof Error ? error.message : "Could not load catalog"); }
  }, []);

  useEffect(() => { void loadCatalog(); }, [loadCatalog]);

  async function showDetail() {
    try { setProduct(track(await request<Product>("/api/products/101"), "GET", "/api/products/{id}", "Product")); setView("detail"); setMessage(""); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Could not load product"); }
  }
  async function showReviews() {
    try { setReviews(track(await request<ReviewsResponse>("/api/products/101/reviews"), "GET", "/api/products/{id}/reviews", "ReviewsResponse")); setView("reviews"); setMessage(""); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Could not load reviews"); }
  }
  async function placeOrder() {
    try { const created = track(await request<Order>("/api/orders", "POST", { items: [{ productId: 101, quantity: 1 }] }), "POST", "/api/orders", "Order"); setOrder(track(await request<Order>(`/api/orders/${created.id}`), "GET", "/api/orders/{id}", "Order")); setView("orders"); setMessage("Order created and reloaded."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Could not create order"); }
  }
  async function runTest(path: string) {
    try { await request<ApiError>(path); setMessage("Unexpected successful response."); }
    catch (error) { setMessage(`Expected response: ${error instanceof Error ? error.message : "request failed"}`); }
  }
  async function createProduct() {
    try { const created = await request<Product>("/api/products", "POST", { name: "Demo monitor", price: 9900, stock: 5 }); track(created, "POST", "/api/products", "Product"); setMessage("Created a product (201)."); await loadCatalog(); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Create failed"); }
  }

  return <main className="min-h-screen bg-slate-950 p-5 text-slate-100 sm:p-10">
    <section className="mx-auto max-w-5xl">
      <header className="mb-8 flex flex-col justify-between gap-5 border-b border-slate-700 pb-6 sm:flex-row sm:items-end">
        <div><p className="text-sm font-bold uppercase tracking-widest text-cyan-300">TraceUsage controlled demo</p><h1 className="mt-2 text-4xl font-black">Shop telemetry playground</h1><p className="mt-2 text-slate-400">Use each view to generate endpoint and browser field-access evidence.</p></div>
        <button className="rounded-lg bg-cyan-400 px-4 py-2 font-bold text-slate-950" onClick={() => void loadCatalog()}>Reload catalog</button>
      </header>
      <nav className="mb-6 flex flex-wrap gap-2" aria-label="Demo views">{(["catalog", "detail", "reviews", "orders", "tests"] as View[]).map((item) => <button key={item} onClick={() => setView(item)} className={`rounded-full px-4 py-2 text-sm font-bold capitalize ${view === item ? "bg-indigo-500 text-white" : "bg-slate-800 text-slate-300"}`}>{item}</button>)}</nav>
      {message && <p className="mb-5 rounded-lg border border-amber-500/50 bg-amber-950/30 p-3 text-amber-100">{message}</p>}

      {view === "catalog" && <section><h2 className="text-2xl font-bold">Catalog</h2><p className="mb-5 text-slate-400">This view intentionally reads product names, prices, images, stock, and supplier names only.</p><div className="mb-5 flex flex-wrap gap-2">{categories.map((category) => <span className="rounded-full bg-slate-800 px-3 py-1 text-sm" key={category.id}>{category.name}</span>)}</div><div className="grid gap-4 sm:grid-cols-3">{products.map((item) => <article className="rounded-xl border border-slate-700 bg-slate-900 p-5" key={item.id}><div className="mb-4 h-24 rounded-lg bg-gradient-to-br from-cyan-500/30 to-indigo-500/30" /><h3 className="font-bold">{item.name}</h3><p className="mt-1 text-cyan-300">{item.currency} {item.price.toLocaleString()}</p><p className="mt-2 text-sm text-slate-400">{item.stock} in stock · {item.supplier.name}</p></article>)}</div></section>}
      {view === "detail" && <section className="demo-card">{product ? <><h2 className="text-3xl font-black">{product.name}</h2><p className="mt-2 text-xl text-cyan-300">{product.currency} {product.price.toLocaleString()}</p><p className="mt-5 text-slate-300">{product.description}</p><dl className="mt-6 grid gap-3 sm:grid-cols-3"><div><dt>Stock</dt><dd>{product.stock}</dd></div><div><dt>Supplier</dt><dd>{product.supplier.name}</dd></div><div><dt>Memory</dt><dd>{product.specifications.memoryGb} GB</dd></div></dl><p className="mt-6 text-sm text-slate-500">Legacy/internal, supplier country/rating, CPU, and storage fields are intentionally not read.</p></> : <button onClick={() => void showDetail()}>Load product 101</button>}</section>}
      {view === "reviews" && <section className="demo-card"><button onClick={() => void showReviews()}>Load product reviews</button>{reviews && <div className="mt-6 grid gap-3">{reviews.reviews.map((review) => <article className="rounded-lg bg-slate-800 p-4" key={review.id}><strong>{"★".repeat(review.rating)} · {review.reviewer.name}</strong><p className="mt-1 text-slate-300">{review.comment}</p></article>)}</div>}</section>}
      {view === "orders" && <section className="demo-card"><button onClick={() => void placeOrder()}>Place order for Laptop</button>{order && <article className="mt-6 rounded-lg bg-slate-800 p-5"><h2 className="font-bold">Order {order.id} · {order.status}</h2><p className="mt-2 text-cyan-300">{order.currency} {order.total.toLocaleString()}</p>{order.items.map((item) => <p className="mt-3" key={item.productId}>{item.productName} × {item.quantity}</p>)}</article>}</section>}
      {view === "tests" && <section className="demo-card"><h2 className="text-2xl font-bold">Endpoint test controls</h2><p className="mb-5 text-slate-400">Generate expected error status codes and a creation event.</p><div className="flex flex-wrap gap-3"><button onClick={() => void createProduct()}>Create product (201)</button><button onClick={() => void runTest("/api/products/999")}>Missing product (404)</button><button onClick={() => void runTest("/api/demo/errors/400")}>Trigger 400</button><button onClick={() => void runTest("/api/demo/errors/500")}>Trigger 500</button></div></section>}
    </section>
  </main>;
}
