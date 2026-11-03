"use client";
import {
  createContext,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from "react";
import { API_BASE } from "@/lib/shop";
export class CartItem {
  constructor(
    public pid: number,
    public quantity: number,
    public name: string,
    public price: number,
    public stock: number = 0,
    public image?: string,
  ) {}
  getSubtotal() {
    return this.price * this.quantity;
  }
}
interface CartContextType {
  cart: CartItem[];
  ready: boolean;
  notice: string;
  setNotice: (message: string) => void;
  bagOpen: boolean;
  setBagOpen: (open: boolean) => void;
  addToCart: (
    pid: number,
    quantity: number,
    name: string,
    price: number,
    stock: number,
    image?: string,
  ) => void;
  updateQuantity: (pid: number, quantity: number) => void;
  removeFromCart: (pid: number) => void;
  getCartTotal: () => number;
  getTotalItems: () => number;
  clearCart: () => void;
}
const CartContext = createContext<CartContextType | undefined>(undefined);
export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [ready, setReady] = useState(false);
  const [notice, setNotice] = useState("");
  const [bagOpen, setBagOpen] = useState(false);
  useEffect(() => {
    let cancelled = false;
    async function restore() {
      try {
        let saved: unknown = [];
        try {
          saved = JSON.parse(localStorage.getItem("shopping_cart") || "[]");
        } catch {
          /* Ignore malformed or unavailable local storage. */
        }
        const stored = Array.isArray(saved) ? saved : [];
        const quantities = new Map<number, number>();
        for (const item of stored) {
          if (
            item &&
            Number.isSafeInteger(item.pid) &&
            item.pid > 0 &&
            Number.isSafeInteger(item.quantity) &&
            item.quantity > 0
          )
            quantities.set(
              item.pid,
              Math.min(999, (quantities.get(item.pid) || 0) + item.quantity),
            );
        }
        const items = await Promise.all(
          [...quantities].map(async ([pid, quantity]) => {
            const res = await fetch(`${API_BASE}/products/${pid}`);
            if (res.status === 404) return null;
            if (!res.ok)
              throw new Error(
                "Unable to restore your bag. Refresh to try again.",
              );
            const p = await res.json();
            return new CartItem(
              pid,
              Math.min(quantity, Math.max(1, p.stock)),
              p.name,
              p.price,
              p.stock,
              p.thumbnailImage,
            );
          }),
        );
        if (!cancelled) {
          setCart(items.filter((item): item is CartItem => item !== null));
          setReady(true);
        }
      } catch {
        if (!cancelled)
          setNotice(
            "Your saved bag could not be loaded. Refresh the page to try again.",
          );
      }
    }
    void restore();
    return () => {
      cancelled = true;
    };
  }, []);
  useEffect(() => {
    if (!ready) return;
    try {
      localStorage.setItem(
        "shopping_cart",
        JSON.stringify(cart.map(({ pid, quantity }) => ({ pid, quantity }))),
      );
    } catch {
      /* Shopping remains usable when storage is unavailable. */
    }
  }, [cart, ready]);
  const addToCart = (
    pid: number,
    quantity: number,
    name: string,
    price: number,
    stock: number,
    image?: string,
  ) => {
    if (!ready || stock <= 0 || !Number.isSafeInteger(quantity) || quantity < 1)
      return;
    setCart((items) => {
      const existing = items.find((i) => i.pid === pid);
      const next = new CartItem(
        pid,
        Math.min(stock, (existing?.quantity || 0) + quantity),
        name,
        price,
        stock,
        image,
      );
      return existing
        ? items.map((i) => (i.pid === pid ? next : i))
        : [...items, next];
    });
    setNotice(`${name} added to your bag.`);
  };
  const removeFromCart = (pid: number) =>
    setCart((items) => items.filter((i) => i.pid !== pid));
  const updateQuantity = (pid: number, quantity: number) => {
    if (!Number.isSafeInteger(quantity)) return;
    if (quantity <= 0) {
      removeFromCart(pid);
      return;
    }
    setCart((items) =>
      items.map((i) =>
        i.pid === pid
          ? new CartItem(
              i.pid,
              Math.min(quantity, Math.max(1, i.stock)),
              i.name,
              i.price,
              i.stock,
              i.image,
            )
          : i,
      ),
    );
  };
  return (
    <CartContext.Provider
      value={{
        cart,
        ready,
        notice,
        setNotice,
        bagOpen,
        setBagOpen,
        addToCart,
        updateQuantity,
        removeFromCart,
        getCartTotal: () => cart.reduce((s, i) => s + i.getSubtotal(), 0),
        getTotalItems: () => cart.reduce((s, i) => s + i.quantity, 0),
        clearCart: () => setCart([]),
      }}
    >
      {children}
    </CartContext.Provider>
  );
}
export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error("CartProvider is required");
  return context;
}
