import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TraceUsage Demo Web",
  description: "Frontend demo for TraceUsage response field tracking",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
