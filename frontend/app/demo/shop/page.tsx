'use client';
import { useEffect, useState } from 'react';
import { demoFetch } from '@/lib/demo';
import { Category, Product } from '@/types';
import ProductCard from '@/components/ProductCard';
import VoucherOffers from '@/components/VoucherOffers';
export default function DemoShop() {
 const [data,setData]=useState<{products:Product[];categories:Category[]}|null>(null),[error,setError]=useState('');
 useEffect(()=>{demoFetch('/catalog').then(setData).catch(e=>setError(e.message));},[]);
 return <main id="main-content" className="page-shell"><h1>Good things for your every day.</h1><p>Sample collection · Private sample inventory · Simulated checkout</p>{error&&<p role="alert">{error} <button onClick={()=>location.reload()}>Retry</button></p>}{!data&&!error&&<p role="status">Loading sample collection…</p>}{data&&<><VoucherOffers/><div className="product-grid">{data.products.map((p,i)=><ProductCard key={p.pid} product={p} categories={data.categories} index={i} demo/>)}</div></>}</main>;
}
