import Link from "next/link";
import { notFound } from "next/navigation";
import { api } from "@/lib/api";
import Catalog from "@/components/Catalog";
export default async function CategoryPage({
  params,
}: {
  params: Promise<{ categorySlug: string }>;
}) {
  const { categorySlug } = await params;
  const match = categorySlug.match(/^(\d+)-/);
  if (!match) notFound();
  const id = Number(match[1]);
  const [categories, products] = await Promise.all([
    api.getCategories(),
    api.getProducts(String(id)),
  ]);
  if (!categories.some((c) => c.catid === id)) notFound();
  return (
    <main id="main-content">
      <div className="shell collection-intro">
        <Link href="/" className="text-link">
          ← Back to all essentials
        </Link>
        <p className="eyebrow">THE ROCKY.TECH COLLECTION</p>
        <h1>Find your kind of everyday.</h1>
        <p className="muted">
          A considered selection for the things you love doing.
        </p>
      </div>
      <Catalog categories={categories} products={products} categoryId={id} />
    </main>
  );
}
