"use client";
import { useState } from "react";
import { api } from "@/lib/api";
import { adminMutation } from "@/lib/admin";
import { everydayEdit } from "@/data/everyday-edit";
import { Notice } from "./ShopUI";
import Icon from "./Icon";
export default function ImportCollection({
  onImported,
}: {
  onImported: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState(false);
  async function importEdit() {
    if (busy) return;
    setBusy(true);
    setError(false);
    let added = 0;
    try {
      const categories = await api.getCategories();
      const products = await api.getProducts();
      for (const item of everydayEdit) {
        if (
          products.some(
            (p) =>
              p.name === item.name &&
              categories.find((c) => c.catid === p.catid)?.name ===
                item.category,
          )
        )
          continue;
        setMessage(`Adding ${item.name}…`);
        let category = categories.find((c) => c.name === item.category);
        if (!category) {
          const response = await adminMutation(
            "categories",
            "POST",
            new URLSearchParams({ name: item.category }),
          );
          category = await response.json();
          categories.push(category!);
        }
        const response = await fetch(`/catalog/${item.image}.jpg`);
        if (!response.ok)
          throw new Error(
            "A product image could not be loaded. Retry to continue.",
          );
        const form = new FormData();
        form.set("catid", String(category!.catid));
        form.set("name", item.name);
        form.set("price", String(item.price));
        form.set("stock", String(item.stock));
        form.set("description", item.description);
        form.set("image", await response.blob(), `${item.image}.jpg`);
        await adminMutation("products", "POST", form);
        added++;
      }
      await onImported();
      setMessage(
        added
          ? `${added} essentials added. The collection is ready to explore.`
          : "The everyday edit is already in your collection. Existing products were kept.",
      );
    } catch (e) {
      setError(true);
      setMessage(
        e instanceof Error
          ? e.message
          : "Import stopped. Retry to continue from the last saved product.",
      );
      await onImported();
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="import-collection">
      <div>
        <h3>The everyday edit</h3>
        <p>
          8 demo essentials with photography, prices, and stock. Existing items
          are kept.
        </p>
      </div>
      <button
        className="button"
        disabled={busy}
        onClick={() => void importEdit()}
      >
        {busy ? "Adding collection…" : "Add demo collection"}
        <Icon name="plus" size={16} />
      </button>
      {message && <Notice error={error}>{message}</Notice>}
    </div>
  );
}
