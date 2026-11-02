import { demoToken, demoFetch, randomKey } from "./demo";
// frontend/lib/api.ts
import { Category, Product } from '@/types';
import { responseError } from './shop';

const API_BASE_URL = typeof window === 'undefined'
  ? `${process.env.BACKEND_URL || 'http://127.0.0.1:8080'}/api`
  : (process.env.NEXT_PUBLIC_API_URL || '/api');

const requireNormalContext = () => { if (demoToken()) throw new Error('Exit demo before using account settings.'); };

// Fetch the masked token from the session-backed endpoint; never mix it with a raw cookie token.
export const getCsrfToken = async (): Promise<string> => {
  requireNormalContext();
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
    credentials: 'include', cache: 'no-store',
  });
  if (!response.ok) throw new Error('Could not verify the session. Please try again.');
  const data = await response.json();
  return data.token;
};

export const api = {
  getCategories: async (): Promise<Category[]> => {
    const res = await fetch(`${API_BASE_URL}/categories`, { cache: 'no-store' });
    if (!res.ok) throw new Error('Failed to fetch categories');
    return res.json();
  },

  getProducts: async (catid?: string): Promise<Product[]> => {
    const url = catid ? `${API_BASE_URL}/products?catid=${catid}` : `${API_BASE_URL}/products`;
    const res = await fetch(url, { cache: 'no-store' });
    if (!res.ok) throw new Error('Failed to fetch products');
    return res.json();
  },

  getProductById: async (id: string): Promise<Product | null> => {
    const res = await fetch(`${API_BASE_URL}/products/${id}`, { cache: 'no-store' });
    if (res.status === 404 || res.status === 400) return null;
    if (!res.ok) throw new Error('Product could not be loaded. Please try again.');
    return res.json();
  },

  // 🚀 新增：用户获取自己的会话
  getMySessions: async () => {
    requireNormalContext();
    const res = await fetch(`${API_BASE_URL}/auth/sessions`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch sessions');
    return res.json();
  },

  // 🚀 新增：用户踢出自己的会话
  kickSession: async (dbSessionId: number) => {
    const csrfToken = await getCsrfToken();
    const res = await fetch(`${API_BASE_URL}/auth/sessions/kick`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-XSRF-TOKEN': csrfToken
      },
      body: new URLSearchParams({ dbSessionId: dbSessionId.toString() }),
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to kick session');
    return res.text();
  },

  // 🚀 新增：管理员获取所有会话
  getAdminSessions: async () => {
    requireNormalContext();
    const res = await fetch(`${API_BASE_URL}/auth/admin/sessions`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch admin sessions');
    return res.json();
  },

  // 🚀 新增：管理员踢出会话
  adminKickSession: async (dbSessionId: number) => {
    const csrfToken = await getCsrfToken();
    const res = await fetch(`${API_BASE_URL}/auth/admin/sessions/kick`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-XSRF-TOKEN': csrfToken
      },
      body: new URLSearchParams({ dbSessionId: dbSessionId.toString() }),
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to kick session');
    return res.text();
  },

  // 🚀 新增：AJAX 验证优惠券接口
  validateVoucher: async (vcode: string): Promise<{ discount: number }> => {
    if(demoToken()) return demoFetch(`/validate-voucher?vcode=${encodeURIComponent(vcode)}`);
    const res = await fetch(`${API_BASE_URL}/checkout/validate-voucher?vcode=${encodeURIComponent(vcode)}`, {
      credentials: 'include' // 保持跨域会话
    });
    if (!res.ok) {
      const errorData = await res.json();
      throw new Error(errorData.error || 'Invalid voucher code');
    }
    return res.json();
  },

  // 🚀 修改：入参改为支持携带 vcode 的 Payload
  checkout: async (payload: { cartItems: { pid: number; quantity: number }[], vcode?: string }): Promise<{ url: string }> => {
    if(demoToken()) {
      const serialized=JSON.stringify(payload); let cached;
      try { cached=JSON.parse(sessionStorage.getItem('demo_checkout')||'null'); } catch {}
      const key=cached?.payload===serialized ? cached.key : randomKey(); sessionStorage.setItem('demo_checkout',JSON.stringify({payload:serialized,key}));
      const result=await demoFetch('/checkout',{method:'POST',body:serialized,headers:{'Idempotency-Key':key}}); sessionStorage.removeItem('demo_checkout'); return result;
    }
    const csrfToken = await getCsrfToken();
    const res = await fetch(`${API_BASE_URL}/checkout/create-session`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken
      },
      body: JSON.stringify(payload), // 这里传入的是 { cartItems: [...], vcode: "..." }
      credentials: 'include',
    });

    if (!res.ok) {
      throw new Error(await responseError(res, 'Checkout could not be started. Please try again.'));
    }
    return res.json();
  },

  getMyOrders: async () => {
    if(demoToken()) return demoFetch("/orders");
    const res = await fetch(`${API_BASE_URL}/orders/my-orders`, {
      cache: 'no-store',
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to fetch orders');
    return res.json();
  },

  getAdminOrders: async () => {
    requireNormalContext();
    const res = await fetch(`${API_BASE_URL}/admin/orders`, {
      cache: 'no-store',
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to fetch admin orders');
    return res.json();
  }
};
