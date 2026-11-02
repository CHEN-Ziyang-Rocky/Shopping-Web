"use client";
import { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/context/CartContext";
import { getCsrfToken } from "@/lib/api";
import { API_BASE, responseError } from "@/lib/shop";
import { Notice, LoadingState } from "./ShopUI";
import Icon from "./Icon";
function PasswordField({
  label,
  id,
  value,
  onChange,
  autoComplete,
}: {
  label: string;
  id: string;
  value: string;
  onChange: (v: string) => void;
  autoComplete: string;
}) {
  const [show, setShow] = useState(false);
  return (
    <div className="password-input">
      <label className="form-field" htmlFor={id}>
        {label}
        <input
          required
          id={id}
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          autoComplete={autoComplete}
        />
      </label>
      <button
        type="button"
        aria-label={`${show ? "Hide" : "Show"} ${label.toLowerCase()}`}
        onClick={() => setShow(!show)}
      >
        {show ? "Hide" : "Show"}
      </button>
    </div>
  );
}
export default function AuthForm({
  mode,
}: {
  mode: "login" | "register" | "password";
}) {
  const register = mode === "register",
    change = mode === "password";
  const { user, loading, login, clearUser } = useAuth();
  const { setBagOpen } = useCart();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [old, setOld] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const rules = [
    ["At least 8 characters", password.length >= 8],
    ["One uppercase letter", /[A-Z]/.test(password)],
    ["One number", /\d/.test(password)],
    ["One special character (!@#$…)", /[!@#$%^&*(),.?":{}|<>]/.test(password)],
  ] as const;
  const valid =
    rules.every(([, met]) => met) &&
    new TextEncoder().encode(password).length <= 72;
  useEffect(() => {
    if (change && !loading && !user && !success) router.replace("/login");
  }, [change, loading, user, success, router]);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    setError("");
    if (mode !== "login" && (!valid || password !== confirm)) {
      setError(
        "Meet all password requirements and make sure your passwords match.",
      );
      return;
    }
    setBusy(true);
    try {
      const token = await getCsrfToken();
      const endpoint = change ? "change-password" : mode;
      const body: Record<string, string> = change
        ? { oldPassword: old, newPassword: password }
        : register
          ? { email: email.trim(), password, confirmPassword: confirm }
          : { email: email.trim(), password };
      const response = await fetch(`${API_BASE}/auth/${endpoint}`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-XSRF-TOKEN": token,
        },
        body: new URLSearchParams(body),
      });
      if (!response.ok)
        throw new Error(
          await responseError(
            response,
            mode === "login"
              ? "Please check your email and password."
              : "Your request could not be completed.",
          ),
        );
      if (mode === "login") {
        const data = await response.json();
        login({ email: data.email, role: data.role });
        const next = new URLSearchParams(window.location.search).get("next");
        if (next === "bag") {
          router.push("/");
          setBagOpen(true);
        } else if (next === "/user/orders" || next === "/user/security")
          router.push(next);
        else router.push(data.role === "ROLE_ADMIN" ? "/admin/products" : "/");
      } else {
        setSuccess(true);
        if (change) clearUser();
        setTimeout(() => router.push("/login"), 2000);
      }
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Connection failed. Please try again.",
      );
      setBusy(false);
    }
  }
  if (change && (loading || (!user && !success))) return <LoadingState />;
  return (
    <main id="main-content" className="auth-layout">
      <aside className="auth-story">
        <div>
          <p className="eyebrow">YOUR EVERYDAY, A LITTLE BETTER.</p>
          <h2>
            Good things.
            <br />
            Even better
            <br />
            with you.
          </h2>
        </div>
        <div className="auth-art">
          <Image
            src="/catalog/headphones.jpg"
            alt="Black headphones on a yellow background"
            fill
            sizes="50vw"
            priority
          />
        </div>
        <p>
          A considered collection of thoughtful tech and everyday essentials,
          all in one little corner.
        </p>
      </aside>
      <section className="auth-content">
        <p className="eyebrow">
          {change
            ? "ACCOUNT SECURITY"
            : register
              ? "MAKE YOURSELF AT HOME"
              : "YOUR ROCKY.TECH ACCOUNT"}
        </p>
        <h1>
          {change
            ? "A fresh start."
            : register
              ? "Hello, good company."
              : "Welcome back."}
        </h1>
        <p>
          {change
            ? "Update your password. You will sign in again afterwards."
            : register
              ? "Create an account to make these essentials yours."
              : "Sign in to your favourites, your bag, and your orders."}
        </p>
        {error && (
          <div style={{ marginTop: 20 }}>
            <Notice error>{error}</Notice>
          </div>
        )}
        {success ? (
          <div style={{ marginTop: 25 }}>
            <Notice>
              {change
                ? "Password updated. Please sign in again."
                : "Your account is ready. Taking you to sign in…"}
            </Notice>
            <Link className="button primary" href="/login">
              Continue to sign in
              <Icon name="arrow" size={17} />
            </Link>
          </div>
        ) : (
          <><p><Link href="/demo">Try Demo</Link> · No registration required. Explore with sample data.</p>
        <form onSubmit={submit}>
            {!change && (
              <label className="form-field" htmlFor="email">
                Email address
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  maxLength={254}
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                />
              </label>
            )}
            {change && (
              <PasswordField
                id="current-password"
                label="Current password"
                value={old}
                onChange={setOld}
                autoComplete="current-password"
              />
            )}
            <PasswordField
              id="password"
              label={change ? "New password" : "Password"}
              value={password}
              onChange={setPassword}
              autoComplete={
                mode === "login" ? "current-password" : "new-password"
              }
            />
            {mode !== "login" && (
              <>
                <div
                  className="password-rules"
                  aria-label="Password requirements"
                >
                  {rules.map(([label, met]) => (
                    <p className={met ? "met" : ""} key={label}>
                      <Icon name={met ? "check" : "minus"} size={12} />
                      {label}
                    </p>
                  ))}
                </div>
                <PasswordField
                  id="confirm-password"
                  label="Confirm password"
                  value={confirm}
                  onChange={setConfirm}
                  autoComplete="new-password"
                />
                {confirm && confirm !== password && (
                  <p className="error-text">Your passwords do not match yet.</p>
                )}
                {new TextEncoder().encode(password).length > 72 && (
                  <p className="error-text">
                    Use a password shorter than 72 bytes.
                  </p>
                )}
              </>
            )}
            <button
              className="button primary full-width"
              type="submit"
              disabled={
                busy || (mode !== "login" && (!valid || password !== confirm))
              }
            >
              {busy
                ? "One moment…"
                : change
                  ? "Update password"
                  : register
                    ? "Create account"
                    : "Sign in"}
              <Icon name="arrow" size={17} />
            </button>
          </form></>
        )}
        {!change && (
          <p className="auth-switch">
            {register ? "Already part of the collection?" : "New around here?"}{" "}
            <Link className="button" href={register ? "/login" : "/register"}>
              {register ? "Sign in" : "Register"}
            </Link>
          </p>
        )}
        <p className="auth-footer-note">
          <Icon
            name="lock"
            size={11}
            style={{ verticalAlign: "middle", marginRight: 5 }}
          />{" "}
          Your account, securely yours.
        </p>
      </section>
    </main>
  );
}
