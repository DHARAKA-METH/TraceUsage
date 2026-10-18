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
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);
  const [categoryProducts, setCategoryProducts] = useState<ProductSummary[]>([]);
  const [createdProductId, setCreatedProductId] = useState<number | null>(null);
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
  async function inspectCategory(id: number) {
    try {
      const [category, catalog] = await Promise.all([request<Category>(`/api/categories/${id}`), request<ProductSummary[]>(`/api/categories/${id}/products`)]);
      setSelectedCategory(track(category, "GET", "/api/categories/{id}", "Category"));
      setCategoryProducts(track(catalog, "GET", "/api/categories/{id}/products", "ProductList"));
      setMessage(`Loaded category ${id} and its products.`);
    } catch (error) { setMessage(error instanceof Error ? error.message : "Could not load category"); }
  }
  async function runTest(path: string) {
    try { await request<ApiError>(path); setMessage("Unexpected successful response."); }
    catch (error) { setMessage(`Expected response: ${error instanceof Error ? error.message : "request failed"}`); }
  }
  async function createProduct() {
    try { const created = track(await request<Product>("/api/products", "POST", { name: "Demo monitor", price: 9900, stock: 5 }), "POST", "/api/products", "Product"); setCreatedProductId(created.id); setMessage(`Created product ${created.id} (201). You can now update or delete it.`); await loadCatalog(); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Create failed"); }
  }
  async function updateProduct() {
    try { const updated = track(await request<Product>("/api/products/101", "PATCH", { name: "Laptop updated", price: 251000, stock: 13 }), "PATCH", "/api/products/{id}", "Product"); setProduct(updated); setMessage("Updated product 101 (200)."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Update failed"); }
  }
  async function deleteCreatedProduct() {
    if (!createdProductId) { setMessage("Create a demo product first, then delete it."); return; }
    try { await request<void>(`/api/products/${createdProductId}`, "DELETE"); setMessage(`Deleted product ${createdProductId} (204).`); setCreatedProductId(null); await loadCatalog(); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Delete failed"); }
  }

  return <main className="min-h-screen bg-slate-950 p-5 text-slate-100 sm:p-10">
    <section className="mx-auto max-w-5xl">
      <header className="mb-8 flex flex-col justify-between gap-5 border-b border-slate-700 pb-6 sm:flex-row sm:items-end">
        <div><p className="text-sm font-bold uppercase tracking-widest text-cyan-300">TraceUsage controlled demo</p><h1 className="mt-2 text-4xl font-black">Shop telemetry playground</h1><p className="mt-2 text-slate-400">Use each view to generate endpoint and browser field-access evidence.</p></div>
        <button className="rounded-lg bg-cyan-400 px-4 py-2 font-bold text-slate-950" onClick={() => void loadCatalog()}>Reload catalog</button>
      </header>
      <nav className="mb-6 flex flex-wrap gap-2" aria-label="Demo views">{(["catalog", "detail", "reviews", "orders", "tests"] as View[]).map((item) => <button key={item} onClick={() => setView(item)} className={`rounded-full px-4 py-2 text-sm font-bold capitalize ${view === item ? "bg-indigo-500 text-white" : "bg-slate-800 text-slate-300"}`}>{item}</button>)}</nav>
      {message && <p className="mb-5 rounded-lg border border-amber-500/50 bg-amber-950/30 p-3 text-amber-100">{message}</p>}

      {view === "catalog" && <section><h2 className="text-2xl font-bold">Catalog and categories</h2><p className="mb-5 text-slate-400">This view reads every available catalog and category field for field-access testing.</p><div className="mb-5 flex flex-wrap gap-2">{categories.map((category) => <button className="rounded-full bg-slate-800 px-3 py-1 text-sm" key={category.id} onClick={() => void inspectCategory(category.id)}>{category.name} — {category.description}</button>)}</div>{selectedCategory && <article className="demo-card mb-5"><h3 className="font-bold">{selectedCategory.id}: {selectedCategory.name}</h3><p>{selectedCategory.description}</p><p className="mt-2 text-sm text-slate-400">Products in category: {categoryProducts.map((item) => item.name).join(", ")}</p></article>}<div className="grid gap-4 sm:grid-cols-3">{products.map((item) => <article className="rounded-xl border border-slate-700 bg-slate-900 p-5" key={item.id}><div className="mb-4 h-24 rounded-lg bg-gradient-to-br from-cyan-500/30 to-indigo-500/30" /><h3 className="font-bold">{item.id}: {item.name}</h3><p className="mt-1 text-cyan-300">{item.currency} {item.price.toLocaleString()}</p><p className="mt-2 text-sm text-slate-400">Image: {item.imageUrl}</p><p className="text-sm text-slate-400">{item.stock} in stock · {item.tags.join(", ")}</p><p className="text-sm text-slate-400">Supplier {item.supplier.id}: {item.supplier.name}, {item.supplier.country}, rating {item.supplier.internalRating}</p></article>)}</div></section>}
      {view === "detail" && <section className="demo-card">{product ? <><h2 className="text-3xl font-black">{product.id}: {product.name}</h2><p className="mt-2 text-xl text-cyan-300">{product.currency} {product.price.toLocaleString()}</p><p className="mt-5 text-slate-300">{product.description}</p><dl className="mt-6 grid gap-3 sm:grid-cols-3"><div><dt>Image</dt><dd>{product.imageUrl}</dd></div><div><dt>Stock / tags</dt><dd>{product.stock} / {product.tags.join(", ")}</dd></div><div><dt>Legacy code</dt><dd>{product.legacyCode}</dd></div><div><dt>Internal catalog reference</dt><dd>{product.internalCatalogReference}</dd></div><div><dt>Supplier</dt><dd>{product.supplier.id} · {product.supplier.name} · {product.supplier.country} · {product.supplier.internalRating}</dd></div><div><dt>Specifications</dt><dd>{product.specifications.cpu} · {product.specifications.memoryGb} GB · {product.specifications.storageGb} GB</dd></div></dl></> : <button onClick={() => void showDetail()}>Load every Product field</button>}</section>}
      {view === "reviews" && <section className="demo-card"><button onClick={() => void showReviews()}>Load product reviews</button>{reviews && <div className="mt-6 grid gap-3"><p>Product ID: {reviews.productId}</p>{reviews.reviews.map((review) => <article className="rounded-lg bg-slate-800 p-4" key={review.id}><strong>{review.id} · {"★".repeat(review.rating)} · {review.reviewer.name}</strong><p className="mt-1 text-slate-300">{review.comment}</p><p className="text-sm text-slate-400">{review.reviewer.email}</p></article>)}</div>}</section>}
      {view === "orders" && <section className="demo-card"><button onClick={() => void placeOrder()}>Place and reload order</button>{order && <article className="mt-6 rounded-lg bg-slate-800 p-5"><h2 className="font-bold">Order {order.id} · {order.status}</h2><p className="mt-2 text-cyan-300">{order.currency} {order.total.toLocaleString()}</p>{order.items.map((item) => <p className="mt-3" key={item.productId}>{item.productId}: {item.productName} × {item.quantity} · {item.unitPrice} · line total {item.lineTotal}</p>)}<p className="mt-3 text-sm text-slate-300">Shipping: {order.shippingAddress.city}, {order.shippingAddress.country} · zone {order.shippingAddress.internalZone}</p><p className="text-sm text-slate-400">Internal fulfilment: {order.internalFulfilmentNote}</p></article>}</section>}
      {view === "tests" && <section className="demo-card"><h2 className="text-2xl font-bold">All endpoint test controls</h2><p className="mb-5 text-slate-400">Use each action to generate endpoint telemetry for successful and expected-error routes.</p><div className="flex flex-wrap gap-3"><button onClick={() => void createProduct()}>Create product (201)</button><button onClick={() => void updateProduct()}>Update product (200)</button><button onClick={() => void deleteCreatedProduct()}>Delete created product (204)</button><button onClick={() => void runTest("/api/products/999")}>Missing product (404)</button><button onClick={() => void runTest("/api/demo/errors/400")}>Trigger 400</button><button onClick={() => void runTest("/api/demo/errors/500")}>Trigger 500</button></div></section>}
    </section>
  </main>;
}
