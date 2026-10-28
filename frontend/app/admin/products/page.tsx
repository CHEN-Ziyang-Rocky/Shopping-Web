"use client";
import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from "@mui/material";
import { api } from "@/lib/api";
import { adminMutation } from "@/lib/admin";
import { money } from "@/lib/shop";
import { Category, Product } from "@/types";
import {
  PageHeading,
  Notice,
  EmptyState,
  LoadingState,
} from "@/components/ShopUI";
import Icon from "@/components/Icon";
import ProductImage from "@/components/ProductImage";
import ImportCollection from "@/components/ImportCollection";
const blank = { catid: "", name: "", price: "", stock: "0", description: "" };
export default function ProductsPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState<number | null>(null);
  const [deleting, setDeleting] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [query, setQuery] = useState("");
  const file = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLElement>(null);
  async function refresh() {
    const [cats, prods] = await Promise.all([
      api.getCategories(),
      api.getProducts(),
    ]);
    setCategories(cats);
    setProducts(prods);
  }
  useEffect(() => {
    Promise.all([api.getCategories(), api.getProducts()])
      .then(([c, p]) => {
        setCategories(c);
        setProducts(p);
      })
      .catch(() =>
        setError("Your collection could not be loaded. Refresh to try again."),
      )
      .finally(() => setLoading(false));
  }, []);
  function reset() {
    setForm(blank);
    setEditing(null);
    if (file.current) file.current.value = "";
  }
  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    setError("");
    setMessage("");
    const image = file.current?.files?.[0];
    if (!editing && !image) {
      setError("Choose a product image before adding this item.");
      return;
    }
    if (
      image &&
      (!["image/jpeg", "image/png", "image/webp"].includes(image.type) ||
        image.size > 10 * 1024 * 1024)
    ) {
      setError("Choose a JPG, PNG, or WebP image smaller than 10 MB.");
      return;
    }
    setBusy(true);
    try {
      const data = new FormData();
      Object.entries(form).forEach(([k, v]) => data.append(k, v.trim()));
      if (image) data.append("image", image);
      await adminMutation(
        editing ? `products/${editing}` : "products",
        editing ? "PUT" : "POST",
        data,
      );
      reset();
      await refresh();
      setMessage(
        editing
          ? "Product updated. Your storefront is up to date."
          : "Product added to your collection.",
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save this product.");
    } finally {
      setBusy(false);
    }
  }
  function edit(p: Product) {
    setEditing(p.pid);
    setForm({
      catid: String(p.catid),
      name: p.name,
      price: String(p.price),
      stock: String(p.stock),
      description: p.description || "",
    });
    setMessage("");
    setError("");
    if (file.current) file.current.value = "";
    formRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }
  async function remove() {
    if (!deleting) return;
    setBusy(true);
    try {
      await adminMutation(`products/${deleting.pid}`, "DELETE");
      await refresh();
      if (editing === deleting.pid) reset();
      setMessage("Product deleted.");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Could not delete this product.",
      );
    } finally {
      setBusy(false);
      setDeleting(null);
    }
  }
  const visible = products.filter((p) =>
    p.name.toLowerCase().includes(query.toLowerCase()),
  );
  return (
    <>
      <PageHeading
        eyebrow="A CONSIDERED COLLECTION"
        title="Products"
        description="Add, refine, and keep track of the essentials in your store."
      />
      <div className="stat-grid">
        <div className="stat-card">
          <p className="eyebrow">PRODUCTS</p>
          <strong>{products.length}</strong>
        </div>
        <div className="stat-card">
          <p className="eyebrow">CATEGORIES</p>
          <strong>{categories.length}</strong>
        </div>
        <div className="stat-card">
          <p className="eyebrow">AVAILABLE UNITS</p>
          <strong>{products.reduce((s, p) => s + p.stock, 0)}</strong>
        </div>
      </div>
      {error && <Notice error>{error}</Notice>}
      {message && <Notice>{message}</Notice>}
      <ImportCollection onImported={refresh} />
      <div className="admin-grid">
        <section className="panel" ref={formRef}>
          <h2>{editing ? "Edit essential" : "Add an essential"}</h2>
          {!categories.length && !loading && (
            <Notice>
              Create a category first, or add the demo collection above.{" "}
              <Link href="/admin/categories" className="text-link">
                Manage categories
              </Link>
            </Notice>
          )}
          <form onSubmit={save}>
            <label className="form-field">
              Category
              <select
                required
                value={form.catid}
                onChange={(e) => setForm({ ...form, catid: e.target.value })}
              >
                <option value="">Choose a collection</option>
                {categories.map((c) => (
                  <option key={c.catid} value={c.catid}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-field">
              Product name
              <input
                required
                maxLength={200}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Give this essential a name"
              />
            </label>
            <div className="form-row">
              <label className="form-field">
                Price (HK$)
                <input
                  type="number"
                  required
                  step="0.01"
                  min="0.01"
                  max="99999999"
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })}
                />
              </label>
              <label className="form-field">
                Available stock
                <input
                  type="number"
                  required
                  step="1"
                  min="0"
                  max="2147483647"
                  value={form.stock}
                  onChange={(e) => setForm({ ...form, stock: e.target.value })}
                />
              </label>
            </div>
            <label className="form-field">
              Description
              <textarea
                required
                rows={4}
                maxLength={10000}
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
                placeholder="What makes this product worth a place in someone's everyday?"
              />
            </label>
            <div className="upload-box">
              <label className="form-field" htmlFor="product-image">
                Product image
              </label>
              <p className="muted">
                {editing
                  ? "Leave empty to keep the current photo."
                  : "A clear product photo makes all the difference."}{" "}
                JPG, PNG, WebP · up to 10 MB.
              </p>
              <input
                id="product-image"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                ref={file}
              />
            </div>
            <div className="form-buttons">
              <button
                className="button primary"
                disabled={busy || !categories.length}
              >
                {busy ? "Saving…" : editing ? "Save changes" : "Add product"}
                <Icon name="plus" size={16} />
              </button>
              {editing && (
                <button
                  className="button"
                  type="button"
                  onClick={reset}
                  disabled={busy}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>
        <section className="panel">
          <h2>
            Your collection <span className="muted">({products.length})</span>
          </h2>
          <div className="search-input" style={{ width: "100%" }}>
            <Icon name="search" size={16} />
            <input
              aria-label="Search inventory"
              placeholder="Find a product…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          {loading ? (
            <LoadingState />
          ) : visible.length ? (
            visible.map((p) => (
              <div className="list-row" key={p.pid}>
                <div className="inventory-photo">
                  <ProductImage
                    src={p.thumbnailImage}
                    alt={p.name}
                    sizes="60px"
                  />
                </div>
                <div className="inventory-info">
                  <h3>{p.name}</h3>
                  <small>
                    {money(p.price)} ·{" "}
                    {p.stock > 0 ? `${p.stock} in stock` : "Sold out"}
                  </small>
                </div>
                <div className="list-actions">
                  <button
                    className="icon-button"
                    aria-label={`Edit ${p.name}`}
                    disabled={busy}
                    onClick={() => edit(p)}
                  >
                    <Icon name="edit" size={16} />
                  </button>
                  <button
                    className="icon-button"
                    aria-label={`Delete ${p.name}`}
                    disabled={busy}
                    onClick={() => setDeleting(p)}
                  >
                    <Icon name="trash" size={16} />
                  </button>
                </div>
              </div>
            ))
          ) : (
            <EmptyState
              icon="bag"
              title={
                query ? "No matching products." : "Your next great collection."
              }
              description={
                query
                  ? "Try a different product name."
                  : "Add your first essential with a photo, description, price, and stock."
              }
            />
          )}
        </section>
      </div>
      <Dialog open={!!deleting} onClose={() => !busy && setDeleting(null)}>
        <DialogTitle>Delete {deleting?.name}?</DialogTitle>
        <DialogContent>
          This cannot be undone. Products referenced by orders cannot be
          deleted; set their stock to zero instead.
        </DialogContent>
        <DialogActions>
          <Button disabled={busy} onClick={() => setDeleting(null)}>
            Keep product
          </Button>
          <Button disabled={busy} color="error" onClick={() => void remove()}>
            Delete product
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
