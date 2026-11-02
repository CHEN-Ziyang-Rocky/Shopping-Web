export const DEMO_TOKEN = 'portfolio_demo_token';
export const demoToken = () => typeof window === 'undefined' ? null : sessionStorage.getItem(DEMO_TOKEN);
const base = `${process.env.NEXT_PUBLIC_API_URL || '/api'}/demo`;
export function randomKey() { return [...crypto.getRandomValues(new Uint8Array(32))].map(n=>n.toString(16).padStart(2,'0')).join(''); }
export async function demoFetch(path: string, init: RequestInit = {}) {
  const res = await fetch(`${base}${path}`, { ...init, credentials:'omit', cache:'no-store', headers:{'Content-Type':'application/json', Authorization:`Bearer ${demoToken() || ''}`, ...init.headers} });
  if (!res.ok) {
    if(res.status===401) { sessionStorage.removeItem(DEMO_TOKEN); sessionStorage.removeItem('portfolio_demo_key'); window.location.assign('/demo'); }
    throw new Error(res.status===429?'Demo limit reached. Retry later or reset your demo.':res.status===401?'Demo expired.':'Demo temporarily unavailable. Please retry.');
  }
  return res.json();
}
export async function startDemo() {
  let key=sessionStorage.getItem('portfolio_demo_key'); if(!key) {key=randomKey();sessionStorage.setItem('portfolio_demo_key',key);}
  const data=await demoFetch('/session',{method:'POST',body:'{}',headers:{'Idempotency-Key':key}});
  sessionStorage.setItem(DEMO_TOKEN,data.token); window.location.assign('/demo/shop');
}
export async function exitDemo() { await demoFetch('/exit',{method:'POST',body:'{}'}); sessionStorage.removeItem(DEMO_TOKEN);sessionStorage.removeItem('portfolio_demo_key');window.location.assign('/demo'); }
