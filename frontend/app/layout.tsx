import type { Metadata } from "next";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import ThemeRegistry from "@/components/ThemeRegistry";
import { CartProvider } from "@/context/CartContext";
import { AuthProvider } from "@/context/AuthContext";
import "./globals.css";
export const metadata: Metadata = {
  title: {
    default: "Rocky.Tech — Good things for your every day",
    template: "%s | Rocky.Tech",
  },
  description:
    "Discover a considered collection of thoughtful tech and everyday essentials. Shop in Hong Kong dollars with secure Stripe checkout.",
};
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <ThemeRegistry>
          <AuthProvider>
            <CartProvider>
              <a href="#main-content" className="skip-link">
                Skip to content
              </a>
              <div className="app-chrome" data-app-chrome>
                <Navbar />
              </div>
              <div className="page-content">{children}</div>
              <Footer />
            </CartProvider>
          </AuthProvider>
        </ThemeRegistry>
      </body>
    </html>
  );
}
