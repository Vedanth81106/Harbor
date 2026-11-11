import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Harbor",
    short_name: "Harbor",
    description: "Push your code. Get a URL.",
    start_url: "/",
    display: "standalone",
    background_color: "#07101b",
    theme_color: "#07101b",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml",
      },
    ],
  };
}
