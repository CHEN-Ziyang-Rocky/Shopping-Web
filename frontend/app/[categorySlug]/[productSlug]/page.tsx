import Link from "next/link";
import { notFound } from "next/navigation";
import { api } from "@/lib/api";
import { money, slug } from "@/lib/shop";
import ProductImage from "@/components/ProductImage";
import ProductDetailActions from "@/components/ProductDetailActions";
import ProductCard from "@/components/ProductCard";
import Icon from "@/components/Icon";
export default async function ProductPage({
  params,
}: {
  params: Promise<{ categorySlug: string; productSlug: string }>;
}) {
  const { productSlug } = await params;
  const [product, categories] = await Promise.all([
    api.getProductById(productSlug),
    api.getCategories(),
  ]);
  if (!product) notFound();
  const category = categories.find((c) => c.catid === product.catid);
  const related = (await api.getProducts())
    .filter((p) => p.pid !== product.pid)
    .sort(
      (a, b) =>
        Number(b.catid === product.catid) - Number(a.catid === product.catid),
    )
    .slice(0, 4);
  return (
    <main id="main-content" className="shell detail-page">
      <nav className="breadcrumbs" aria-label="Breadcrumb">
        <Link href="/">Home</Link>
        <Icon name="chevron" size={12} />
        <Link href={`/${slug(product.catid, category?.name || "collection")}`}>
          {category?.name || "Collection"}
        </Link>
        <Icon name="chevron" size={12} />
        <span>{product.name}</span>
      </nav>
      <article className="product-detail">
        <div className="detail-photo">
          <ProductImage
            src={product.largeImage}
            alt={product.name}
            priority
            sizes="(max-width:700px) 100vw, 55vw"
          />
          <span className="detail-photo-caption">
            THE ROCKY.TECH EVERYDAY EDIT
          </span>
        </div>
        <div className="detail-copy">
          <p className="eyebrow">{category?.name || "THE COLLECTION"}</p>
          <h1>{product.name}</h1>
          <p className="detail-price">{money(product.price)}</p>
          <p className="detail-description">{product.description}</p>
          <ProductDetailActions product={product} />
          <div className="detail-assurances">
            <p>
              <Icon name="shield" size={18} /> Secure payment through Stripe
            </p>
            <p>
              <Icon name="bag" size={18} /> Your bag is saved on this device
            </p>
          </div>
          <details className="product-info" open>
            <summary>About this collection</summary>
            <p>
              Thoughtfully selected essentials for work, play, and everything in
              between. This is a portfolio demo store; product photography is
              illustrative and checkout uses Stripe test payments.
            </p>
          </details>
          <details className="product-info">
            <summary>Payment & orders</summary>
            <p>
              All prices are in Hong Kong dollars. Sign in to check out, apply a
              valid voucher in your bag, and view confirmed payment status in My
              orders.
            </p>
          </details>
        </div>
      </article>
      {related.length > 0 && (
        <section className="related">
          <div className="section-heading">
            <div>
              <p className="eyebrow">GOOD COMPANY</p>
              <h2>Better together.</h2>
            </div>
            <Link className="text-link" href="/#collection">
              All essentials <Icon name="arrow" size={17} />
            </Link>
          </div>
          <div className="product-grid">
            {related.map((p) => (
              <ProductCard key={p.pid} product={p} categories={categories} />
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
