import { ClosingCta } from "@/components/landing/closing-cta";
import { Hero } from "@/components/landing/hero";
import { HowItWorks } from "@/components/landing/how-it-works";
import { Product } from "@/components/landing/product";
import { WhoItsFor } from "@/components/landing/who-its-for";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export default function Home() {
  return (
    <div id="top" className="flex min-h-svh flex-col">
      <SiteHeader />
      <main id="main" tabIndex={-1} className="flex-1">
        <Hero />
        <Product />
        <HowItWorks />
        <WhoItsFor />
      </main>
      <SiteFooter />
    </div>
  );
}
