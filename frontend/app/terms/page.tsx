import type { Metadata } from "next";
import Link from "next/link";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "Terms",
};

export default function TermsPage() {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main id="main" tabIndex={-1} className="flex-1 bg-bg">
        <article className="mx-auto w-full max-w-3xl px-6 py-16 sm:py-24">
          <p className="text-[11px] uppercase tracking-[0.18em] text-secondary">
            Legal
          </p>
          <h1 className="mt-4 text-[clamp(2.2rem,5vw,3.4rem)] font-medium leading-[1.05] tracking-[-0.025em] text-fg">
            Terms
          </h1>
          <p className="mt-4 text-[15px] text-muted">
            Last updated: {new Date().getFullYear()}
          </p>

          <div className="mt-10 space-y-8 text-[16px] leading-relaxed text-muted">
            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                Overview
              </h2>
              <p className="mt-3">
                Harbor is provided as an early, evolving project. These terms
                are a simple placeholder and will be refined as the service
                matures.
              </p>
            </section>

            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                Using Harbor
              </h2>
              <p className="mt-3">
                You&rsquo;re responsible for the repositories, code, and content
                you deploy. Please use Harbor lawfully and avoid deploying
                anything you don&rsquo;t have the right to run or share.
              </p>
            </section>

            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                Availability
              </h2>
              <p className="mt-3">
                The service may change, be interrupted, or be unavailable at
                times, especially while it&rsquo;s under active development.
              </p>
            </section>

            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                Contact
              </h2>
              <p className="mt-3">
                Questions? Reach out via the links in the{" "}
                <Link href="/#top" className="text-secondary hover:text-fg transition-colors">
                  site footer
                </Link>
                .
              </p>
            </section>
          </div>
        </article>
      </main>
      <SiteFooter />
    </div>
  );
}
