"use client";
import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { LoadingState } from "@/components/ShopUI";
export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const path = usePathname();
  useEffect(() => {
    if (!loading && !user)
      router.replace(`/login?next=${encodeURIComponent(path)}`);
  }, [loading, user, router, path]);
  if (loading || !user) return <LoadingState label="Opening your account…" />;
  return (
    <main id="main-content" className="page-shell">
      <nav className="account-nav" aria-label="Account navigation">
        <Link
          href="/user/orders"
          className={path === "/user/orders" ? "active" : ""}
        >
          My orders
        </Link>
        {user.role!=="ROLE_DEMO" && <><Link
          href="/user/security"
          className={path === "/user/security" ? "active" : ""}
        >
          Account & devices
        </Link>
        <Link href="/change-password">Change password</Link></>}
      </nav>
      {children}
    </main>
  );
}
