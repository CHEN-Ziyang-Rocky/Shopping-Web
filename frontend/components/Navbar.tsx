"use client";
import { useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Drawer, Snackbar } from "@mui/material";
import Icon from "./Icon";
import ProductImage from "./ProductImage";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api";
import { money } from "@/lib/shop";
import VoucherOffers from "./VoucherOffers";
import { EmptyState } from "./ShopUI";
export default function Navbar() {
  const {
    cart,
    ready,
    updateQuantity,
    removeFromCart,
    getCartTotal,
    getTotalItems,
    bagOpen,
    setBagOpen,
    notice,
    setNotice,
  } = useCart();
  const { user, loading, logout } = useAuth();
  const path = usePathname();
  const accountMenu = useRef<HTMLDetailsElement>(null);
  const [checking, setChecking] = useState(false);
  const [vcode, setVcode] = useState("");
  const [appliedCode, setAppliedCode] = useState("");
  const [discount, setDiscount] = useState(0);
  const [validating, setValidating] = useState(false);
  const [message, setMessage] = useState("");
  const [checkoutError, setCheckoutError] = useState("");
  const voucherRequest = useRef(0);
  const [mobile, setMobile] = useState(false);
  const closeMenu = () => {
    if (accountMenu.current) accountMenu.current.open = false;
  };
  async function validateVoucher(selectedCode?: string) {
    const request = ++voucherRequest.current;
    const code = (selectedCode ?? vcode).trim();
    setVcode(code);
    if (!user) {
      setAppliedCode("");
      setDiscount(0);
      setMessage("Sign in, then apply this code before checkout.");
      return;
    }
    setValidating(true);
    setMessage("");
    try {
      const data = await api.validateVoucher(code);
      if (request !== voucherRequest.current) return;
      setDiscount(data.discount);
      setAppliedCode(code);
      setMessage("Voucher applied to your bag.");
    } catch (error) {
      if (request !== voucherRequest.current) return;
      setDiscount(0);
      setAppliedCode("");
      setMessage(
        error instanceof Error
          ? error.message
          : "This voucher is not available.",
      );
    } finally {
      if (request === voucherRequest.current) setValidating(false);
    }
  }
  async function checkout() {
    if (!user || !cart.length || checking) return;
    setChecking(true);
    setCheckoutError("");
    try {
      const response = await api.checkout({
        cartItems: cart.map(({ pid, quantity }) => ({ pid, quantity })),
        vcode: appliedCode || undefined,
      });
      window.location.assign(response.url);
    } catch (error) {
      setCheckoutError(
        error instanceof Error
          ? error.message
          : "Checkout could not be started. Please try again.",
      );
      setChecking(false);
    }
  }
  return (
    <>
      <div className="announcement">
        <span className="announcement-full">{user?.role === "ROLE_DEMO" ? <Link href="/demo/shop">Demo collection · Sample products</Link> : <>Thoughtful essentials for a better everyday. <Link href="/demo">Try Demo</Link></>}</span>
        <span className="announcement-mobile">{user?.role === "ROLE_DEMO" ? <Link href="/demo/shop">Demo collection</Link> : 'A portfolio project'}</span>
        <nav className="project-links" aria-label="Project links">
          <a href="https://imrocky.com/"><span aria-hidden="true">← </span>Portfolio</a>
          <a
            href="https://github.com/CHEN-Ziyang-Rocky/Shopping-Web"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="E-Commerce Web Application source on GitHub (opens in a new tab)"
          >GitHub <span aria-hidden="true">↗</span></a>
        </nav>
      </div>
      <header className="site-header">
        <div className="header-inner shell">
          <Link href={user?.role === "ROLE_DEMO" ? "/demo/shop" : "/"} className="wordmark" aria-label="Rocky.Tech home">
            rocky<span className="brand-dot">.</span>tech
            <span className="wordmark-star">✳</span>
          </Link>
          <nav className="desktop-nav" aria-label="Main navigation">
            <Link className={path === "/" ? "active" : ""} href={user?.role === "ROLE_DEMO" ? "/demo/shop" : "/#collection"}>
              The collection
            </Link>
            <Link
              href="/user/orders"
              className={path === "/user/orders" ? "active" : ""}
            >
              My orders
            </Link>
            {user?.role === "ROLE_ADMIN" && (
              <Link
                href="/admin/products"
                className={path.startsWith("/admin") ? "active" : ""}
              >
                Store manager <span>↗</span>
              </Link>
            )}
          </nav>
          <div className="header-actions">
            <Link
              href="/?q=#collection"
              className="icon-button header-search"
              aria-label="Search products"
            >
              <Icon name="search" />
            </Link>
            {user ? (
              <details
                className="account-menu"
                ref={accountMenu}
                onKeyDown={(e) => {
                  if (e.key === "Escape") closeMenu();
                }}
              >
                <summary className="account-trigger" aria-label="Account menu">
                  <Icon name="user" />
                  <span>Account</span>
                </summary>
                <div className="account-popover">
                  <p className="eyebrow">YOUR ACCOUNT</p>
                  <p className="account-email">{user.email}</p>
                  <Link href="/user/orders" onClick={closeMenu}>
                    <Icon name="box" />
                    My orders
                  </Link>
                  <Link href="/user/security" onClick={closeMenu}>
                    <Icon name="shield" />
                    Account & devices
                  </Link>
                  <Link href="/change-password" onClick={closeMenu}>
                    <Icon name="lock" />
                    Change password
                  </Link>
                  {user.role === "ROLE_ADMIN" && (
                    <Link href="/admin/products" onClick={closeMenu}>
                      <Icon name="grid" />
                      Store manager
                    </Link>
                  )}
                  <button
                    onClick={() => {
                      closeMenu();
                      void logout();
                    }}
                  >
                    <Icon name="logout" />
                    Sign out
                  </button>
                </div>
              </details>
            ) : (
              <> <Link className="account-trigger" href="/login">
                <Icon name="user" />
                <span>{loading ? "Account" : "Sign in"}</span>
              </Link><Link className="button" href="/register">Register</Link></>
            )}
            <button
              className="bag-trigger"
              onClick={() => setBagOpen(true)}
              aria-label={`Open shopping bag, ${getTotalItems()} items`}
            >
              <Icon name="bag" />
              <span>Bag</span>
              <span className="bag-count">{getTotalItems()}</span>
            </button>
            <button
              className="icon-button mobile-toggle"
              aria-label="Toggle navigation"
              aria-expanded={mobile}
              onClick={() => setMobile(!mobile)}
            >
              <Icon name={mobile ? "close" : "menu"} />
            </button>
          </div>
        </div>
        {mobile && (
          <nav
            className="mobile-nav"
            aria-label="Mobile navigation"
            onClick={() => setMobile(false)}
          >
            <Link href={user?.role === "ROLE_DEMO" ? "/demo/shop" : "/#collection"}>The collection</Link>
            <Link href="/user/orders">My orders</Link>
            <Link href="/user/security">Account & devices</Link>
            {!user && <Link className="button" href="/register">Register</Link>}
            {user?.role === "ROLE_ADMIN" && (
              <Link href="/admin/products">Store manager</Link>
            )}
          </nav>
        )}
      </header>
      <Drawer
        anchor="right"
        open={bagOpen}
        onClose={() => setBagOpen(false)}
        slotProps={{
          paper: {
            className: "bag-drawer",
            sx: { width: { xs: "100%", sm: 470 }, maxWidth: "100%" },
            role: "dialog",
            "aria-modal": true,
            "aria-labelledby": "bag-title",
          },
        }}
      >
        <div className="bag-heading">
          <div>
            <p className="eyebrow">YOUR EVERYDAY ESSENTIALS</p>
            <h2 id="bag-title">
              Shopping bag <span>({getTotalItems()})</span>
            </h2>
          </div>
          <button
            className="icon-button"
            aria-label="Close shopping bag"
            onClick={() => setBagOpen(false)}
          >
            <Icon name="close" />
          </button>
        </div>
        {cart.length === 0 ? (
          <div
            onClick={(e) => {
              if ((e.target as HTMLElement).closest("a")) setBagOpen(false);
            }}
          >
            <EmptyState
              icon="bag"
              title="Room for something good."
              description={
                ready
                  ? "Your bag is waiting for a new favourite."
                  : "Loading your saved bag…"
              }
              href="/#collection"
              label="Explore the collection"
            />
          </div>
        ) : (
          <>
            <div className="bag-items">
              {cart.map((item) => (
                <article className="bag-item" key={item.pid}>
                  <div className="bag-item-photo">
                    <ProductImage
                      src={item.image}
                      alt={item.name}
                      sizes="90px"
                    />
                  </div>
                  <div className="bag-item-info">
                    <h3>{item.name}</h3>
                    <p>{money(item.price)}</p>
                    {item.stock <= 0 && (
                      <p className="error-text">
                        Currently sold out. Remove to continue.
                      </p>
                    )}
                    <div className="quantity-control">
                      <button
                        aria-label={`Decrease ${item.name} quantity`}
                        onClick={() =>
                          updateQuantity(item.pid, item.quantity - 1)
                        }
                      >
                        <Icon name="minus" size={14} />
                      </button>
                      <span aria-label={`Quantity ${item.quantity}`}>
                        {item.quantity}
                      </span>
                      <button
                        aria-label={`Increase ${item.name} quantity`}
                        disabled={item.quantity >= item.stock}
                        onClick={() =>
                          updateQuantity(item.pid, item.quantity + 1)
                        }
                      >
                        <Icon name="plus" size={14} />
                      </button>
                    </div>
                  </div>
                  <div className="bag-item-end">
                    <button
                      className="icon-button"
                      aria-label={`Remove ${item.name}`}
                      onClick={() => removeFromCart(item.pid)}
                    >
                      <Icon name="close" size={17} />
                    </button>
                    <strong>{money(item.getSubtotal())}</strong>
                  </div>
                </article>
              ))}
            </div>
            <div className="bag-summary">
              <VoucherOffers
                compact
                disabled={validating || checking}
                onSelect={(code) => void validateVoucher(code)}
              />
              <form
                className="voucher-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  void validateVoucher();
                }}
              >
                <label className="sr-only" htmlFor="voucher">
                  Voucher code
                </label>
                <input
                  id="voucher"
                  placeholder="Have a voucher code?"
                  value={vcode}
                  onChange={(e) => {
                    voucherRequest.current++;
                    setVcode(e.target.value.toUpperCase());
                    setDiscount(0);
                    setAppliedCode("");
                    setMessage("");
                    setValidating(false);
                  }}
                />
                <button type="submit" disabled={!vcode.trim() || validating}>
                  {validating ? "Checking…" : "Apply"}
                </button>
              </form>
              {message && (
                <p
                  className={appliedCode ? "success-text" : "error-text"}
                  role="status"
                >
                  {message}
                </p>
              )}
              <div className="summary-row">
                <span>Subtotal</span>
                <span>{money(getCartTotal())}</span>
              </div>
              {discount > 0 && (
                <div className="summary-row success-text">
                  <span>Voucher discount</span>
                  <span>− {money(Math.min(discount, getCartTotal()))}</span>
                </div>
              )}
              <div className="summary-row total">
                <span>Total</span>
                <span>{money(Math.max(0, getCartTotal() - discount))}</span>
              </div>
              {checkoutError && (
                <p role="alert" className="notice error">
                  {checkoutError}
                </p>
              )}
              {user ? (
                <button
                  className="button primary full-width"
                  disabled={
                    checking || validating || cart.some((i) => i.stock <= 0)
                  }
                  onClick={() => void checkout()}
                >
                  {checking
                    ? (user?.role==="ROLE_DEMO" ? "Creating simulated order…" : "Opening secure checkout…")
                    : (user?.role==="ROLE_DEMO" ? "Place simulated order" : "Continue to checkout")}
                  <Icon name="arrow" size={18} />
                </button>
              ) : (
                <Link
                  href="/login?next=bag"
                  className="button primary full-width"
                  onClick={() => setBagOpen(false)}
                >
                  Sign in to checkout
                  <Icon name="arrow" size={18} />
                </Link>
              )}
              <p className="checkout-note">
                <Icon name="lock" size={13} /> {user?.role==="ROLE_DEMO" ? "Simulated checkout · No card details · HKD" : "Secure Stripe checkout · HKD"}
              </p>
              <p className="demo-note">
                {user?.role==="ROLE_DEMO" ? "Test payments only. No real charges. Stripe is not called." : "Demo store. Use Stripe test payments only."}
              </p>
            </div>
          </>
        )}
      </Drawer>
      <Snackbar
        open={!!notice}
        autoHideDuration={4500}
        onClose={() => setNotice("")}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <div className="toast" role="status">
          <Icon name="check" size={19} />
          <span>{notice}</span>
          <button
            onClick={() => {
              setNotice("");
              setBagOpen(true);
            }}
          >
            View bag
          </button>
          <button
            className="icon-button"
            aria-label="Dismiss notification"
            onClick={() => setNotice("")}
          >
            <Icon name="close" size={16} />
          </button>
        </div>
      </Snackbar>
    </>
  );
}
