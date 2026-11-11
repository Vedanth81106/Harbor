import { Hero } from "@/components/hero";
import { HowItWorks } from "@/components/how-it-works";
import { SiteHeader } from "@/components/site-header";

export default function Home() {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main>
        <Hero />
        <HowItWorks />
      </main>
    </div>
  );
}
