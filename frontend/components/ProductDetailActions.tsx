"use client";
import { useState } from "react";
import { useCart } from "@/context/CartContext";
import { Product } from "@/types";
import Icon from "./Icon";
export default function ProductDetailActions({
  product,
}: {
  product: Product;
}) {
  const [quantity, setQuantity] = useState(1);
  const { cart, addToCart, ready, setBagOpen } = useCart();
  const remaining = Math.max(
    0,
    product.stock - (cart.find((i) => i.pid === product.pid)?.quantity || 0),
  );
  const qty = Math.min(quantity, Math.max(1, remaining));
  return (
    <>
      <p className={`stock-status ${product.stock > 0 ? "" : "sold-out"}`}>
        <span className="small-dot" />
        {product.stock > 0
          ? `${product.stock} available · Ready for your everyday`
          : "Currently sold out"}
      </p>
      <div className="detail-actions">
        <div className="quantity-control">
          <button
            aria-label="Decrease quantity"
            disabled={qty <= 1}
            onClick={() => setQuantity(Math.max(1, qty - 1))}
          >
            <Icon name="minus" size={16} />
          </button>
          <input
            aria-label="Quantity"
            type="number"
            min="1"
            max={Math.max(1, remaining)}
            value={qty}
            onChange={(e) =>
              setQuantity(
                Math.max(1, Math.min(remaining, Number(e.target.value) || 1)),
              )
            }
          />
          <button
            aria-label="Increase quantity"
            disabled={qty >= remaining}
            onClick={() => setQuantity(qty + 1)}
          >
            <Icon name="plus" size={16} />
          </button>
        </div>
        <button
          className="button primary"
          disabled={!ready || remaining <= 0}
          onClick={() =>
            addToCart(
              product.pid,
              qty,
              product.name,
              product.price,
              product.stock,
              product.thumbnailImage,
            )
          }
        >
          <Icon name="bag" size={19} />
          {product.stock <= 0
            ? "Sold out"
            : remaining <= 0
              ? "All available stock in bag"
              : "Add to bag"}
        </button>
      </div>
      {cart.some((i) => i.pid === product.pid) && (
        <button className="text-link" onClick={() => setBagOpen(true)}>
          View your shopping bag <Icon name="arrow" size={17} />
        </button>
      )}
    </>
  );
}
