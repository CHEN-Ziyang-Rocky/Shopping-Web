import Link from "next/link";
import Image from "next/image";
import { api } from "@/lib/api";
import Catalog from "@/components/Catalog";
import Icon from "@/components/Icon";
export default async function Home({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const [categories, products, search] = await Promise.all([
    api.getCategories(),
    api.getProducts(),
    searchParams,
  ]);
  return (
    <main id="main-content">
      <section className="hero shell">
        <div className="hero-copy">
          <p className="eyebrow">
            <span className="small-dot" /> THE EVERYDAY, UPGRADED
          </p>
          <h1>
            Good things.
            <br />
            For your <span>every day.</span>
          </h1>
          <p>
            Thoughtful tech. Beautiful objects. The little things that make
            work, play, and everything in between feel better.
          </p>
          <Link className="button primary" href="#collection">
            Explore the collection <Icon name="arrow" size={18} />
          </Link>
          <div className="hero-note">
            <span className="note-line" /> Carefully chosen. Simply loved.
          </div>
        </div>
        <div className="hero-visual">
          <Image
            src="/catalog/headphones.jpg"
            alt="Black over-ear headphones on a warm yellow background"
            fill
            priority
            sizes="(max-width: 700px) 100vw, 55vw"
          />
          <span className="hero-edition">THE EVERYDAY EDIT — VOL. 01</span>
          <div className="hero-product">
            <div>
              <span>LESS NOISE. MORE YOU.</span>
              <strong>Find your everyday rhythm.</strong>
            </div>
            <Link
              href="#collection"
              className="round-link"
              aria-label="Explore the collection"
            >
              <Icon name="arrow" size={23} />
            </Link>
          </div>
        </div>
      </section>
      <div className="benefits shell">
        <div>
          <Icon name="grid" />
          <span>
            <strong>A considered collection</strong>
            <small>Useful things, thoughtfully selected</small>
          </span>
        </div>
        <div>
          <Icon name="shield" />
          <span>
            <strong>Secure checkout</strong>
            <small>Payments handled by Stripe</small>
          </span>
        </div>
        <div>
          <Icon name="bag" />
          <span>
            <strong>Make it your everyday</strong>
            <small>All prices in Hong Kong dollars</small>
          </span>
        </div>
      </div>
      <Catalog
        key={search.q || ""}
        categories={categories}
        products={products}
        initialQuery={search.q || ""}
      />
      <section className="editorial shell">
        <div className="editorial-photo">
          <Image
            src="/catalog/desk.jpg"
            alt="A thoughtfully arranged workspace"
            fill
            sizes="(max-width: 700px) 100vw, 50vw"
          />
        </div>
        <div className="editorial-copy">
          <p className="eyebrow">A LITTLE INTENTION GOES A LONG WAY</p>
          <h2>
            Make space for
            <br />
            the good stuff.
          </h2>
          <p>
            A desk that inspires you. Music that moves you. An everyday
            companion that just works. Start with something small.
          </p>
          <Link href="#collection" className="text-link">
            Find your next favourite <Icon name="arrow" size={18} />
          </Link>
        </div>
      </section>
    </main>
  );
}
