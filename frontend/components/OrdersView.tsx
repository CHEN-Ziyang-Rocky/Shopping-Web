"use client";
import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import { money } from "@/lib/shop";
import { Order } from "@/types";
import { PageHeading, Notice, LoadingState, EmptyState } from "./ShopUI";
import Icon from "./Icon";
export default function OrdersView({ admin = false }: { admin?: boolean }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  useEffect(() => {
    (admin ? api.getAdminOrders() : api.getMyOrders())
      .then((data: Order[]) =>
        setOrders([...data].sort((a, b) => b.createdAt - a.createdAt)),
      )
      .catch(() =>
        setError("Orders could not be loaded. Please refresh to try again."),
      )
      .finally(() => setLoading(false));
  }, [admin]);
  const visible = orders.filter(
    (o) =>
      (status === "ALL" || o.status === status) &&
      `${o.id} ${o.items.map((i) => i.name || i.pid).join(" ")}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const paid = orders.filter((o) => o.status === "PAID");
  return (
    <>
      <PageHeading
        eyebrow={admin ? "YOUR STORE AT A GLANCE" : "YOUR LITTLE COLLECTION"}
        title={admin ? "All orders" : "My orders"}
        description={
          admin
            ? "An overview of every order, payment status, and purchase in your store."
            : "The good things you picked, all in one place."
        }
      />
      {admin && (
        <div className="stat-grid">
          <div className="stat-card">
            <p className="eyebrow">ORDERS</p>
            <strong>{orders.length}</strong>
          </div>
          <div className="stat-card">
            <p className="eyebrow">PAID ORDERS</p>
            <strong>{paid.length}</strong>
          </div>
          <div className="stat-card">
            <p className="eyebrow">CONFIRMED TOTAL</p>
            <strong>{money(paid.reduce((s, o) => s + o.totalPrice, 0))}</strong>
          </div>
        </div>
      )}
      {error && <Notice error>{error}</Notice>}
      {loading ? (
        <LoadingState label="Finding your orders…" />
      ) : !error && orders.length === 0 ? (
        <section className="panel">
          <EmptyState
            icon="bag"
            title={
              admin
                ? "Your first order is on its way."
                : "Your story starts here."
            }
            description={
              admin
                ? "New purchases and their payment status will appear here."
                : "You have not placed an order yet. Find an everyday essential to make your own."
            }
            href={admin ? "/admin/products" : "/#collection"}
            label={admin ? "Manage the collection" : "Explore the collection"}
          />
        </section>
      ) : (
        !error && (
          <>
            <div className="filter-bar">
              <div className="search-input">
                <Icon name="search" size={17} />
                <input
                  aria-label="Search orders"
                  placeholder="Search order number or product…"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </div>
              <select
                aria-label="Filter orders by payment status"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                <option value="ALL">All statuses</option>
                {[...new Set(orders.map((o) => o.status))].map((s) => (
                  <option key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            {visible.length === 0 && (
              <EmptyState
                icon="search"
                title="No matching orders."
                description="Try a different order number or payment status."
              />
            )}
            {visible.map((order) => (
              <article className="panel order-card" key={order.id}>
                <header className="order-header">
                  <div>
                    <h2>Order #{String(order.id).padStart(4, "0")}</h2>
                    <p>
                      {new Date(order.createdAt).toLocaleString("en-HK", {
                        dateStyle: "medium",
                        timeStyle: "short",
                      })}
                      {admin && ` · Customer #${order.userid}`}
                    </p>
                  </div>
                  <span className={`status-pill ${order.status.toLowerCase()}`}>
                    {order.status.charAt(0) +
                      order.status.slice(1).toLowerCase()}
                  </span>
                </header>
                {order.items.map((item) => (
                  <div className="order-line" key={item.id}>
                    <span>
                      {item.name || `Product #${item.pid}`}
                      <small>
                        {item.quantity} × {money(item.price)}
                      </small>
                    </span>
                    <span>{money(item.quantity * item.price)}</span>
                  </div>
                ))}
                {(order.discountAmount || 0) > 0 && (
                  <div className="order-line success-text">
                    <span>Voucher {order.voucherCode}</span>
                    <span>− {money(order.discountAmount!)}</span>
                  </div>
                )}
                <footer className="order-total">
                  <span>
                    Total <small className="muted">({order.currency})</small>
                  </span>
                  <strong>{money(order.totalPrice)}</strong>
                </footer>
              </article>
            ))}
          </>
        )
      )}
    </>
  );
}
