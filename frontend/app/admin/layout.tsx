"use client";
import { useEffect } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import Icon from "@/components/Icon";
import { LoadingState } from "@/components/ShopUI";
const links = [
  ["products", "Products", "bag"],
  ["categories", "Categories", "grid"],
  ["orders", "All orders", "box"],
  ["sessions", "Sessions", "shield"],
];
export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const path = usePathname();
  useEffect(() => {
    if (!loading && (!user || user.role !== "ROLE_ADMIN"))
      router.replace("/login");
  }, [loading, user, router]);
  if (loading || !user || user.role !== "ROLE_ADMIN")
    return <LoadingState label="Opening your store manager…" />;
  return (
    <div id="main-content" className="admin-shell">
      <aside className="admin-sidebar">
        <p className="eyebrow">STORE MANAGER</p>
        <nav aria-label="Store management">
          {links.map(([url, name, icon]) => (
            <Link
              key={url}
              href={`/admin/${url}`}
              className={path === `/admin/${url}` ? "active" : ""}
            >
              <Icon name={icon} size={17} />
              {name}
            </Link>
          ))}
        </nav>
        <Link className="text-link" href="/">
          Back to storefront <Icon name="arrow" size={14} />
        </Link>
        <p className="admin-notice">
          A thoughtful store starts behind the scenes. Manage your collection,
          orders, and account sessions here.
        </p>
      </aside>
      <main className="admin-body">{children}</main>
    </div>
  );
}
