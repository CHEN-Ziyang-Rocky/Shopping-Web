// frontend/types/index.ts

export interface Category {
  catid: number; // 🚀 这里把 id 改回了 catid
  name: string;
}

export interface Product {
  pid: number;
  catid: number;
  name: string;
  price: number;
  description?: string;
  stock: number;
  thumbnailImage?: string;
  largeImage?: string;
}

export interface User {
  userid: number;
  email: string;
  role: string;
}

export interface OrderItem {
  id: number;
  pid: number;
  quantity: number;
  price: number;
  name?: string;
}

export interface Order {
  id: number;
  userid: number;
  totalPrice: number;
  currency: string;
  merchantEmail: string;
  status: string;
  transactionId?: string;
  createdAt: number;
  items: OrderItem[];
  voucherCode?: string;
  discountAmount?: number;
}

// 多会话管理实体类型
export interface UserSession {
  id: number;
  userid: number;
  current: boolean;
  ipAddress: string;
  loginTime: number;
  valid: boolean;
}
