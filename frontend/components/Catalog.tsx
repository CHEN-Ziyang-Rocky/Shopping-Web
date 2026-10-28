"use client";
import { useState } from "react";
import Link from "next/link";
import { Category, Product } from "@/types";
import { slug } from "@/lib/shop";
import Icon from "./Icon";
import ProductCard from "./ProductCard";
import { EmptyState } from "./ShopUI";
export default function Catalog({
  categories,
  products,
  categoryId,
  initialQuery = "",
}: {
  categories: Category[];
  products: Product[];
  categoryId?: number;
  initialQuery?: string;
}) {
  const [query, setQuery] = useState(initialQuery);
  const [sort, setSort] = useState("curated");
  const [inStock, setInStock] = useState(false);
  const filtered = products.filter(
    (p) =>
      (!inStock || p.stock > 0) &&
      `${p.name} ${p.description || ""} ${categories.find((c) => c.catid === p.catid)?.name || ""}`
        .toLowerCase()
        .includes(query.trim().toLowerCase()),
  );
  if (sort === "price-low") filtered.sort((a, b) => a.price - b.price);
  if (sort === "price-high") filtered.sort((a, b) => b.price - a.price);
  if (sort === "name") filtered.sort((a, b) => a.name.localeCompare(b.name));
  return (
    <section id="collection" className="catalog shell">
      <div className="section-heading">
        <div>
          <p className="eyebrow">GOOD DESIGN. EVERY DAY.</p>
          <h2>
            {categoryId
              ? categories.find((c) => c.catid === categoryId)?.name
              : "Meet your new essentials."}
          </h2>
        </div>
        <p className="muted">A little less ordinary. A lot more you.</p>
      </div>
      <nav className="collection-tabs" aria-label="Product categories">
        <Link href="/#collection" className={!categoryId ? "active" : ""}>
          All essentials <span>{categoryId ? "" : products.length}</span>
        </Link>
        {categories.map((c) => (
          <Link
            href={`/${slug(c.catid, c.name)}#collection`}
            key={c.catid}
            className={categoryId === c.catid ? "active" : ""}
          >
            {c.name}
          </Link>
        ))}
      </nav>
      <div className="catalog-toolbar">
        <div className="search-input">
          <Icon name="search" size={18} />
          <input
            aria-label="Search this collection"
            placeholder="Find something you'll love…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          {query && (
            <button
              className="icon-button"
              aria-label="Clear search"
              onClick={() => setQuery("")}
            >
              <Icon name="close" size={16} />
            </button>
          )}
        </div>
        <div className="catalog-options">
          <label className="stock-filter">
            <input
              type="checkbox"
              checked={inStock}
              onChange={(e) => setInStock(e.target.checked)}
            />{" "}
            In stock
          </label>
          <select
            aria-label="Sort products"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            <option value="curated">Featured</option>
            <option value="price-low">Price: low to high</option>
            <option value="price-high">Price: high to low</option>
            <option value="name">Name: A–Z</option>
          </select>
        </div>
      </div>
      <p className="results-count" aria-live="polite">
        {filtered.length} {filtered.length === 1 ? "essential" : "essentials"}
        {query && ` matching “${query}”`}
      </p>
      {filtered.length ? (
        <div className="product-grid">
          {filtered.map((p, i) => (
            <ProductCard
              product={p}
              categories={categories}
              key={p.pid}
              index={i}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon="search"
          title={
            products.length ? "No matches, yet." : "Good things are on the way."
          }
          description={
            products.length
              ? "Try another search or turn off the stock filter."
              : "Our collection is being prepared. Come back soon."
          }
        />
      )}
    </section>
  );
}
