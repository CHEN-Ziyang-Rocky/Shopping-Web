'use client';
import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Product } from '@/types';
import { demoFetch } from '@/lib/demo';
import { money } from '@/lib/shop';
import ProductImage from '@/components/ProductImage';
import ProductDetailActions from '@/components/ProductDetailActions';
export default function DemoProduct() {
 const {id}=useParams();const [p,setP]=useState<Product|null>(null),[error,setError]=useState('');
 useEffect(()=>{demoFetch(`/products/${id}`).then(setP).catch(e=>setError(e.message));},[id]);
 return <main id="main-content" className="page-shell"><Link href="/demo/shop">← Sample collection</Link>{error&&<p role="alert">{error}</p>}{!p&&!error&&<p>Loading product…</p>}{p&&<section className="product-detail"><div className="detail-photo"><ProductImage src={p.largeImage} alt={p.name}/></div><div className="detail-copy"><p className="eyebrow">SAMPLE PRODUCT</p><h1>{p.name}</h1><p>{money(p.price)}</p><p>{p.description}</p><ProductDetailActions product={p}/></div></section>}</main>;
}
