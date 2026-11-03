"use client";
import { useState, useEffect } from "react";
import { API_BASE, money } from "@/lib/shop";
import Icon from "./Icon";
type Offer = { code: string; discount: number };
export default function VoucherOffers({
  compact = false,
  onSelect,
  disabled = false,
}: {
  compact?: boolean;
  onSelect?: (code: string) => void;
  disabled?: boolean;
}) {
  const [offers, setOffers] = useState<Offer[]>([]);
  const [copied, setCopied] = useState("");
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_BASE}/vouchers`, {
      cache: "no-store",
      signal: controller.signal,
    })
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then(setOffers)
      .catch(() => {});
    return () => controller.abort();
  }, []);
  async function choose(code: string) {
    if (onSelect) {
      onSelect(code);
      return;
    }
    try {
      await navigator.clipboard.writeText(code);
      setCopied(code);
      setError("");
    } catch {
      setError(`Enter ${code} in your shopping bag to use this voucher.`);
    }
  }
  if (!offers.length) return null;
  return (
    <section
      className={`voucher-offers ${compact ? "compact" : ""}`}
      aria-label="Available shopping vouchers"
    >
      <div className="voucher-heading">
        <div>
          <p className="eyebrow">A LITTLE SOMETHING FOR YOU</p>
          <h3>
            {compact
              ? "Choose a voucher"
              : "Good things come with a little extra."}
          </h3>
        </div>
        {!compact && (
          <p>Pick a code. Add your favourites. Enjoy a little off.</p>
        )}
      </div>
      <div className="voucher-cards">
        {offers.map((o) => (
          <article className="voucher-card" key={o.code}>
            <div>
              <strong>{money(o.discount)} off</strong>
              <code>{o.code}</code>
            </div>
            <button
              type="button"
              disabled={disabled}
              className="button"
              aria-label={`${onSelect ? "Apply" : "Copy"} ${o.code} voucher`}
              onClick={() => void choose(o.code)}
            >
              {onSelect ? "Use" : copied === o.code ? "Copied" : "Copy code"}
              <Icon name={copied === o.code ? "check" : "plus"} size={14} />
            </button>
          </article>
        ))}
      </div>
      <p className="voucher-terms">
        One voucher per order · Discount is capped at your subtotal · While
        codes remain available
      </p>
      {copied && !onSelect && (
        <p className="success-text" role="status">
          {copied} copied. Paste it into the voucher field in your bag.
        </p>
      )}
      {error && (
        <p role="status" className="error-text">
          {error}
        </p>
      )}
    </section>
  );
}
