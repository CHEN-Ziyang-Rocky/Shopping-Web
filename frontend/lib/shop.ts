import { Category, Product } from "@/types";
export const money = (value: number) =>
  new Intl.NumberFormat("en-HK", { style: "currency", currency: "HKD" }).format(
    value,
  );
export const slug = (id: number, name: string) =>
  `${id}-${
    name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "") || "item"
  }`;
export const productUrl = (product: Product, categories: Category[]) =>
  `/${slug(product.catid, categories.find((c) => c.catid === product.catid)?.name || "collection")}/${slug(product.pid, product.name)}`;
export const API_BASE = process.env.NEXT_PUBLIC_API_URL || "/api";
export async function responseError(response: Response, fallback: string) {
  const text = await response.text();
  try {
    const body = JSON.parse(text);
    return body.error || body.message || fallback;
  } catch {
    return text.length < 220 && !text.includes("<")
      ? text || fallback
      : fallback;
  }
}
