export type Supplier = { id: number; name: string; country: string; internalRating: number };
export type Product = {
  id: number; name: string; price: number; currency: string; description: string; imageUrl: string;
  stock: number; legacyCode: string; internalCatalogReference: string; tags: string[];
  supplier: Supplier; specifications: { cpu: string; memoryGb: number; storageGb: number };
};
export type ProductSummary = Pick<Product, "id" | "name" | "price" | "currency" | "imageUrl" | "stock" | "tags" | "supplier">;
export type Category = { id: number; name: string; description: string };
export type ReviewsResponse = { productId: number; reviews: Array<{ id: string; rating: number; comment: string; reviewer: { name: string; email: string } }> };
export type Order = { id: string; status: string; total: number; currency: string; items: Array<{ productId: number; productName: string; quantity: number; unitPrice: number; lineTotal: number }> };
export type ApiError = { status: number; message: string };
