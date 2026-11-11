const waterBands = [
  { top: 1.5, left: 2, right: 6, opacity: 0.13 },
  { top: 4, left: 10, right: 1, opacity: 0.1 },
  { top: 6.5, left: 4, right: 12, opacity: 0.09 },
  { top: 9, left: 16, right: 3, opacity: 0.12 },
  { top: 12, left: 1, right: 8, opacity: 0.08 },
  { top: 15.5, left: 12, right: 2, opacity: 0.1 },
  { top: 19, left: 6, right: 14, opacity: 0.07 },
  { top: 23, left: 20, right: 4, opacity: 0.09 },
  { top: 28, left: 2, right: 10, opacity: 0.06 },
  { top: 33, left: 14, right: 2, opacity: 0.08 },
  { top: 39, left: 5, right: 16, opacity: 0.05 },
  { top: 46, left: 22, right: 6, opacity: 0.06 },
  { top: 54, left: 3, right: 12, opacity: 0.04 },
  { top: 63, left: 16, right: 4, opacity: 0.05 },
  { top: 73, left: 6, right: 18, opacity: 0.03 },
  { top: 84, left: 24, right: 8, opacity: 0.03 },
];

export function HarborScene() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 -z-10 overflow-hidden"
    >
      {/* sky and water: one continuous gradient, horizon sits at 63% */}
      <div className="scene-sky absolute inset-0" />

      {/* horizon: a hairline plus a soft bloom, cooler on the left, warmer under the light */}
      <div className="scene-horizon absolute inset-x-0 top-[63%] h-px" />
      <div className="scene-horizon-glow absolute inset-x-0 top-[calc(63%-3rem)] h-24 blur-lg" />

      {/* water: irregular crests, denser and brighter near the horizon */}
      <div className="absolute inset-x-0 bottom-0 top-[63%] [mask-image:linear-gradient(to_bottom,black_0%,black_45%,transparent_92%)]">
        <div className="scene-tint absolute inset-0" />
        <div className="tide absolute inset-0 [--tide-shift:6px]">
          {waterBands.map((band, i) => (
            <div
              key={i}
              className="scene-crest absolute h-px"
              style={{
                top: `${band.top}%`,
                left: `${band.left}%`,
                right: `${band.right}%`,
                opacity: `calc(${band.opacity} * var(--crest-mult))`,
              }}
            />
          ))}
        </div>
      </div>

      {/* the light */}
      <div className="absolute bottom-[37%] right-0 sm:right-[4%]">
        <svg
          viewBox="0 0 160 360"
          fill="none"
          className="h-[clamp(130px,20vh,185px)] w-auto overflow-visible sm:h-[clamp(220px,54vh,500px)]"
        >
          <defs>
            <linearGradient
              id="harbor-beam"
              gradientUnits="userSpaceOnUse"
              x1="80"
              y1="96"
              x2="-980"
              y2="0"
            >
              <stop offset="0" stopColor="#d9455f" stopOpacity="0.42" />
              <stop offset="0.07" stopColor="#c22b45" stopOpacity="0.19" />
              <stop offset="0.3" stopColor="#a81430" stopOpacity="0.08" />
              <stop offset="0.68" stopColor="#800b1f" stopOpacity="0.025" />
              <stop offset="1" stopColor="#800b1f" stopOpacity="0" />
            </linearGradient>

            <radialGradient id="harbor-lamp">
              <stop offset="0" stopColor="#f4dde1" stopOpacity="0.9" />
              <stop offset="0.25" stopColor="#d9455f" stopOpacity="0.45" />
              <stop offset="1" stopColor="#c22b45" stopOpacity="0" />
            </radialGradient>

            <filter id="beam-soft" x="-60%" y="-160%" width="220%" height="420%">
              <feGaussianBlur stdDeviation="9" />
            </filter>
          </defs>

          <g className="beam-sweep scene-beam">
            <polygon
              points="80,96 -980,-70 -980,70"
              fill="url(#harbor-beam)"
              filter="url(#beam-soft)"
            />
          </g>

          {/* tower */}
          <polygon
            points="42,360 118,360 102,120 58,120"
            style={{ fill: "var(--scene-tower)" }}
          />
          <polygon
            points="42,360 118,360 102,120 58,120"
            fill="none"
            strokeWidth="1"
            style={{ stroke: "var(--scene-tower-rim)" }}
          />
          <rect x="57" y="172" width="47" height="8" style={{ fill: "var(--scene-band)" }} />
          <rect x="52" y="234" width="57" height="9" style={{ fill: "var(--scene-band)" }} />
          <rect x="47" y="296" width="66" height="10" style={{ fill: "var(--scene-band)" }} />
          <rect x="72" y="196" width="4" height="7" style={{ fill: "var(--scene-porthole)" }} />
          <rect x="72" y="262" width="4" height="7" style={{ fill: "var(--scene-porthole)" }} />

          {/* gallery, lantern room, roof */}
          <rect x="50" y="112" width="60" height="9" rx="1.5" style={{ fill: "var(--scene-tower)" }} />
          <rect x="62" y="78" width="36" height="34" style={{ fill: "var(--scene-lantern)" }} />
          <rect
            x="62"
            y="78"
            width="36"
            height="34"
            fill="none"
            strokeWidth="1"
            style={{ stroke: "var(--scene-lantern-line)" }}
          />
          <polygon points="80,42 60,78 100,78" style={{ fill: "var(--scene-tower)" }} />
          <path
            d="M80 42V30"
            strokeWidth="2.5"
            strokeLinecap="round"
            style={{ stroke: "var(--scene-tower)" }}
          />

          {/* the lamp */}
          <circle className="lamp-glow" cx="80" cy="96" r="26" fill="url(#harbor-lamp)" />
          <circle cx="80" cy="96" r="2.4" style={{ fill: "var(--scene-lamp-core)" }} />
        </svg>

        {/* reflection: soft, broken by the crests */}
        <div className="scene-reflection absolute left-1/2 top-full h-[clamp(50px,9vh,95px)] w-[38%] -translate-x-1/2 bg-[linear-gradient(to_bottom,rgba(194,43,69,0.38),rgba(194,43,69,0.12)_48%,transparent)] blur-[3px] [mask-image:repeating-linear-gradient(to_bottom,black_0px,black_3px,transparent_3px,transparent_8px)]" />
      </div>
    </div>
  );
}
