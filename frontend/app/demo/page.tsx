'use client';
import { useState } from 'react';
import Link from 'next/link';
import { startDemo } from '@/lib/demo';
export default function DemoEntry() {
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 return <main id="main-content" className="page-shell"><section className="panel" style={{maxWidth:720,margin:'3rem auto'}}><p className="eyebrow">INTERACTIVE PORTFOLIO</p><h1>Explore the everyday edit.</h1><p>No registration required. Explore with sample data.</p><p>Browse sample products, try voucher <strong>DEMO10</strong>, and place a simulated order in your own temporary workspace.</p><p><strong>Test payments only. No real charges.</strong> Checkout is simulated; no card details are requested and Stripe is not called.</p><p>Your normal login and shopping bag are preserved. By default, demo expires after 60 minutes idle, 24 hours total, or service restart.</p>{error&&<p role="alert">Demo temporarily unavailable. {error} Retry below or visit the project overview.</p>}<button className="button primary" disabled={busy} onClick={async()=>{setBusy(true);setError('');try{await startDemo();}catch(e){setError(e instanceof Error?e.message:'Please retry.');sessionStorage.removeItem('portfolio_demo_key');setBusy(false);}}}>{busy?'Preparing demo…':'Try Demo'}</button><p><Link href="/login">Sign in</Link> · <Link className="button" href="/register">Register</Link> · <a href="https://github.com/CHEN-Ziyang-Rocky/Shopping-Web">Project overview & GitHub</a></p></section></main>;
}
