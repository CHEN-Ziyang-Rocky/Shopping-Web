"use client";
import Icon from "@/components/Icon";
export default function ErrorPage({
  reset,
}: {
  error: Error;
  reset: () => void;
}) {
  return (
    <main id="main-content" className="error-page">
      <p className="eyebrow">LET’S TRY THAT AGAIN</p>
      <h1>A small pause.</h1>
      <p>
        We could not load this page. Your bag is still saved on this device.
      </p>
      <button className="button primary" onClick={reset}>
        Try again
        <Icon name="arrow" size={17} />
      </button>
    </main>
  );
}
