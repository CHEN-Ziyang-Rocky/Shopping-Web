"use client";
import { Product } from "@/types";
import { useCart } from "@/context/CartContext";
import Icon from "./Icon";
export default function AddToCartButton({
  product,
  className,
}: {
  product: Product;
  className?: string;
}) {
  const { cart, addToCart, ready } = useCart();
  const full =
    (cart.find((item) => item.pid === product.pid)?.quantity || 0) >=
    product.stock;
  return (
    <button
      className={className || "quick-add"}
      disabled={!ready || product.stock <= 0 || full}
      aria-label={
        full && product.stock > 0
          ? `All available ${product.name} are in your bag`
          : `Add ${product.name} to bag`
      }
      title={full ? "All available stock is in your bag" : "Add to bag"}
      onClick={() =>
        addToCart(
          product.pid,
          1,
          product.name,
          product.price,
          product.stock,
          product.thumbnailImage,
        )
      }
    >
      <Icon name={full ? "check" : "plus"} size={19} />
      <span>{product.stock <= 0 ? "Sold out" : full ? "In bag" : "Add"}</span>
    </button>
  );
}
