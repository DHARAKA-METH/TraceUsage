import path from "node:path";
import type { NextConfig } from "next";

const repoRoot = path.resolve(__dirname, "..");

const nextConfig: NextConfig = {
  transpilePackages: ["@traceusage/browser"],
  turbopack: {
    root: repoRoot,
    resolveAlias: {
      "@traceusage/browser": path.resolve(
        repoRoot,
        "traceusage-browser-sdk/dist/index.js",
      ),
    },
  },
};

export default nextConfig;