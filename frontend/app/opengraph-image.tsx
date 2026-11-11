import { ImageResponse } from "next/og";

export const alt = "Harbor — Push your code. Get a URL.";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          width: "100%",
          height: "100%",
          background: "#07101b",
          backgroundImage:
            "linear-gradient(180deg, #07101b 0%, #0a1d2d 62%, #0d2944 100%)",
          padding: "72px 80px",
          color: "#ffffff",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div
            style={{
              display: "flex",
              width: 20,
              height: 20,
              borderRadius: 999,
              background: "#c22b45",
            }}
          />
          <div
            style={{
              display: "flex",
              fontSize: 30,
              fontWeight: 600,
              letterSpacing: -0.5,
            }}
          >
            Harbor
          </div>
        </div>

        <div style={{ display: "flex", flexGrow: 1 }} />

        <div
          style={{
            display: "flex",
            flexDirection: "column",
            fontSize: 104,
            fontWeight: 600,
            lineHeight: 0.95,
            letterSpacing: -4,
          }}
        >
          <div style={{ display: "flex" }}>Push your code.</div>
          <div style={{ display: "flex", color: "#c22b45" }}>Get a URL.</div>
        </div>

        <div style={{ display: "flex", flexGrow: 1 }} />

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            borderTop: "1px solid rgba(255,255,255,0.14)",
            paddingTop: 24,
            fontSize: 24,
            color: "#b1b9c5",
          }}
        >
          <div style={{ display: "flex" }}>
            Connect a repository. Get a live address.
          </div>
          <div style={{ display: "flex", color: "#1297b5" }}>
            harbor
          </div>
        </div>
      </div>
    ),
    size,
  );
}
