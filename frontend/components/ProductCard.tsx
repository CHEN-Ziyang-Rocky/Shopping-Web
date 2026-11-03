import Link from "next/link";
import { Category, Product } from "@/types";
import { money, productUrl } from "@/lib/shop";
import AddToCartButton from "./AddToCartButton";
import ProductImage from "./ProductImage";
export default function ProductCard({
  product,
  categories,
  index = 0,
}: {
  product: Product;
  categories: Category[];
  index?: number;
}) {
  const category = categories.find((c) => c.catid === product.catid);
  const href = productUrl(product, categories);
  return (
    <article className="product-card">
      <Link
        href={href}
        className="product-photo"
        aria-label={`View ${product.name}`}
      >
        <ProductImage
          src={product.largeImage || product.thumbnailImage}
          alt={product.name}
          priority={index < 4}
        />
        {product.stock === 0 && <span className="photo-label">Sold out</span>}
        {product.stock > 0 && product.stock <= 5 && (
          <span className="photo-label">Only {product.stock} left</span>
        )}
        <span className="photo-view">Take a closer look ↗</span>
      </Link>
      <div className="product-meta">
        <p className="eyebrow">{category?.name || "The collection"}</p>
        <Link href={href}>
          <h3>{product.name}</h3>
        </Link>
        <div className="product-bottom">
          <span className="price">{money(product.price)}</span>
          <AddToCartButton product={product} />
        </div>
      </div>
    </article>
  );
}
