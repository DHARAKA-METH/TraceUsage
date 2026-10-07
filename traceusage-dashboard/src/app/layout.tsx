import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TraceUsage Dashboard",
  description: "Developer dashboard for TraceUsage endpoint analytics",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
