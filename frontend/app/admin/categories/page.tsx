"use client";
import { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from "@mui/material";
import { api } from "@/lib/api";
import { adminMutation } from "@/lib/admin";
import { Category } from "@/types";
import {
  PageHeading,
  Notice,
  EmptyState,
  LoadingState,
} from "@/components/ShopUI";
import Icon from "@/components/Icon";
export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState<Category | null>(null);
  useEffect(() => {
    api
      .getCategories()
      .then(setCategories)
      .catch(() =>
        setError("Categories could not be loaded. Refresh to try again."),
      )
      .finally(() => setLoading(false));
  }, []);
  async function add(e: React.FormEvent) {
    e.preventDefault();
    if (busy || !name.trim()) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await adminMutation(
        "categories",
        "POST",
        new URLSearchParams({ name: name.trim() }),
      );
      setCategories(await api.getCategories());
      setName("");
      setMessage("Category added. It is now available when creating products.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not add category.");
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    if (!deleting) return;
    setBusy(true);
    setError("");
    try {
      await adminMutation(`categories/${deleting.catid}`, "DELETE");
      setCategories(await api.getCategories());
      setMessage("Category deleted.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not delete category.");
    } finally {
      setBusy(false);
      setDeleting(null);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="ORGANISE YOUR COLLECTION"
        title="Categories"
        description="Help shoppers find their favourites with clear, simple collections."
      />
      {error && <Notice error>{error}</Notice>}
      {message && <Notice>{message}</Notice>}
      <div className="admin-grid">
        <section className="panel">
          <h2>Add a category</h2>
          <form onSubmit={add}>
            <label className="form-field">
              Category name
              <input
                required
                maxLength={100}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Desk & Living"
              />
              <small>
                Choose a short name that makes sense to your shoppers.
              </small>
            </label>
            <button
              className="button primary full-width"
              disabled={busy || !name.trim()}
            >
              {busy ? "Saving…" : "Add category"}
              <Icon name="plus" size={17} />
            </button>
          </form>
        </section>
        <section className="panel">
          <h2>
            Your categories <span className="muted">({categories.length})</span>
          </h2>
          {loading ? (
            <LoadingState />
          ) : categories.length ? (
            categories.map((c) => (
              <div className="list-row" key={c.catid}>
                <div>
                  <h3>{c.name}</h3>
                  <small>Collection #{c.catid}</small>
                </div>
                <button
                  className="icon-button"
                  aria-label={`Delete ${c.name}`}
                  disabled={busy}
                  onClick={() => setDeleting(c)}
                >
                  <Icon name="trash" size={17} />
                </button>
              </div>
            ))
          ) : (
            <EmptyState
              icon="grid"
              title="Start with a collection."
              description="Add a category, then give it some great products."
            />
          )}
        </section>
      </div>
      <Dialog open={!!deleting} onClose={() => !busy && setDeleting(null)}>
        <DialogTitle>Delete {deleting?.name}?</DialogTitle>
        <DialogContent>
          Categories containing products cannot be deleted. This action cannot
          be undone.
        </DialogContent>
        <DialogActions>
          <Button disabled={busy} onClick={() => setDeleting(null)}>
            Keep category
          </Button>
          <Button color="error" disabled={busy} onClick={() => void remove()}>
            Delete category
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
