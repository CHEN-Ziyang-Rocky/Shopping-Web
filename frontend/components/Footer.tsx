import Link from "next/link";
import Icon from "./Icon";
export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="shell footer-main">
        <div className="footer-brand">
          <Link href="/" className="wordmark">
            rocky<span className="brand-dot">.</span>tech
            <span className="wordmark-star">✳</span>
          </Link>
          <p>
            A little thought.
            <br />A better everyday.
          </p>
        </div>
        <div>
          <p className="eyebrow">EXPLORE</p>
          <Link href="/#collection">The collection</Link>
          <Link href="/user/orders">My orders</Link>
        </div>
        <div>
          <p className="eyebrow">YOUR CORNER</p>
          <Link href="/user/security">Account &amp; devices</Link>
          <Link href="/login">Sign in / Create account</Link>
        </div>
        <div className="footer-payment">
          <Icon name="shield" size={24} />
          <strong>A safe space to shop.</strong>
          <p>
            Secure payments with Stripe.
            <br />
            All prices in HKD.
          </p>
        </div>
      </div>
      <div className="footer-bottom shell">
        <span>
          © {new Date().getFullYear()} Rocky.Tech. Curated with intention.
        </span>
        <span>Portfolio store · Test payments only</span>
        <span>
          HONG KONG <span className="small-dot" />
        </span>
      </div>
    </footer>
  );
}
