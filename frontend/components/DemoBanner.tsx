'use client';
import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { demoFetch, exitDemo } from '@/lib/demo';
export default function DemoBanner() {
 const {user}=useAuth();const path=usePathname();
 useEffect(()=>{if(user?.role==='ROLE_DEMO' && !path.startsWith('/demo') && path!=='/user/orders') window.location.assign('/demo/shop');},[user,path]);const [busy,setBusy]=useState(false),[error,setError]=useState(''),[confirmReset,setConfirmReset]=useState(false);
 if(user?.role!=='ROLE_DEMO')return null;
 const action=async(reset:boolean)=>{setBusy(true);try{if(reset){await demoFetch('/reset',{method:'POST',body:'{}'});window.location.assign('/demo/shop');}else await exitDemo();}catch(e){setError(e instanceof Error?e.message:'Please retry.');setBusy(false);}};
 return <div className="panel" style={{margin:'1rem',padding:'1rem',display:'flex',gap:'1rem',flexWrap:'wrap',alignItems:'center'}}><span>Demo mode · Sample data</span><Link href="/demo/shop">Demo collection</Link><span>Test payments only. No real charges. Simulated checkout · DEMO10 saves HK$10.</span><button disabled={busy} className="text-link" onClick={()=>setConfirmReset(true)}>Reset demo</button>{confirmReset&&<span>Restore your sample bag, orders and inventory? <button disabled={busy} className="text-link" onClick={()=>action(true)}>Confirm reset</button> <button className="text-link" onClick={()=>setConfirmReset(false)}>Cancel</button></span>}<button disabled={busy} className="text-link" onClick={()=>action(false)}>Exit demo</button>{error&&<span role="alert">{error}</span>}</div>;
}
