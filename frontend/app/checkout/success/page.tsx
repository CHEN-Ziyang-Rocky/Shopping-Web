"use client";

import {
  Box,
  Typography,
  Button,
  Alert,
  CircularProgress,
} from "@mui/material";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api";

function SuccessContent() {
  const sessionId = useSearchParams().get("session_id");
  const [status, setStatus] = useState("PENDING");
  const [needsAuth, setNeedsAuth] = useState(false);
  const [message, setMessage] = useState("Waiting for payment confirmation…");

  useEffect(() => {
    if (!sessionId) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    const controller = new AbortController();
    const check = async (attempt: number) => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/orders/payment-status?sessionId=${encodeURIComponent(sessionId)}`,
          {
            credentials: "include",
            cache: "no-store",
            signal: controller.signal,
          },
        );
        if (cancelled) return;
        if (response.status === 401) {
          setNeedsAuth(true);
          setMessage("Please sign in to view your payment status.");
          return;
        }
        if (!response.ok)
          throw new Error("Unable to confirm payment. Please check My Orders.");
        const data = await response.json();
        if (cancelled) return;
        setStatus(data.status);
        if (data.status === "PAID") {
          setMessage(
            "Your payment has been confirmed. Thank you for your purchase.",
          );
          return;
        }
        if (data.status !== "PENDING") {
          setMessage("Payment was not completed. Your cart has been kept.");
          return;
        }
        if (attempt < 14) timer = setTimeout(() => check(attempt + 1), 2000);
        else
          setMessage(
            "Payment confirmation is taking longer than expected. Check My Orders shortly.",
          );
      } catch (error) {
        if (!cancelled)
          setMessage(
            error instanceof Error
              ? error.message
              : "Unable to confirm payment.",
          );
      }
    };
    void check(0);
    return () => {
      cancelled = true;
      controller.abort();
      clearTimeout(timer);
    };
  }, [sessionId]);

  return (
    <>
      <span className="empty-icon" aria-hidden="true">
        {status === "PAID" ? "✓" : "↗"}
      </span>
      <p className="eyebrow">YOUR ORDER, ONE STEP CLOSER</p>
      <Typography variant="h3" gutterBottom fontWeight="bold">
        {status === "PAID"
          ? "Good things are yours."
          : "Checking your payment."}
      </Typography>
      <Alert severity={status === "PAID" ? "success" : "info"} sx={{ mb: 4 }}>
        {sessionId
          ? message
          : "No payment session was provided. View My Orders to check your purchases."}
      </Alert>
      <Box className="form-buttons">
        {needsAuth && (
          <Button
            variant="text"
            size="large"
            component={Link}
            href="/login?next=%2Fuser%2Forders"
          >
            Sign in
          </Button>
        )}
        <Button variant="contained" size="large" component={Link} href="/">
          Continue Shopping
        </Button>
        <Button
          variant="outlined"
          size="large"
          component={Link}
          href="/user/orders"
        >
          View My Orders
        </Button>
      </Box>
    </>
  );
}

export default function CheckoutSuccessPage() {
  return (
    <Box component="main" id="main-content" className="panel payment-result">
      <Suspense fallback={<CircularProgress />}>
        <SuccessContent />
      </Suspense>
    </Box>
  );
}
