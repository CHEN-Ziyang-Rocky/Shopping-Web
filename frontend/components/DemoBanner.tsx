'use client';
import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { demoFetch, exitDemo } from '@/lib/demo';
export default function DemoBanner() {
 const {user}=useAuth();const path=usePathname();
 useEffect(()=>{if(user?.role==='ROLE_DEMO' && !path.startsWith('/demo') && path!=='/user/orders') window.location.assign('/demo/shop');},[user,path]);const [busy,setBusy]=useState(false),[error,setError]=useState(''),[confirmReset,setConfirmReset]=useState(false);
 if(user?.role!=='ROLE_DEMO')return null;
 const action=async(reset:boolean)=>{setBusy(true);try{if(reset){await demoFetch('/reset',{method:'POST',body:'{}'});window.location.assign('/demo/shop');}else await exitDemo();}catch(e){setError(e instanceof Error?e.message:'Please retry.');setBusy(false);}};
 return <section className="demo-toolbar" aria-label="Demo controls">
  <div className="demo-status"><strong>Demo mode · Sample data</strong><span>Test payments only. No real charges.</span></div>
  <div className="demo-actions"><button disabled={busy} className="text-link" onClick={()=>setConfirmReset(true)}>Reset demo</button><button disabled={busy} className="text-link" onClick={()=>action(false)}>Exit demo</button></div>
  {confirmReset&&<div className="demo-confirm">Restore your sample bag, orders and inventory? <button disabled={busy} className="text-link" onClick={()=>action(true)}>Confirm reset</button><button className="text-link" onClick={()=>setConfirmReset(false)}>Cancel</button></div>}
  {error&&<span className="demo-error" role="alert">{error}</span>}
 </section>;
}
