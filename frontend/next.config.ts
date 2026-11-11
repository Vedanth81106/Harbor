import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  turbopack: {
    root: path.join(__dirname),
  },
  // Next 16 writes an AGENTS.md into the app directory; the repo already has one.
  agentRules: false,
  devIndicators: false,
};

export default nextConfig;
