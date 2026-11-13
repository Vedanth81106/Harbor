import type { Metadata } from "next";
import Link from "next/link";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "Privacy",
};

export default function PrivacyPage() {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main id="main" tabIndex={-1} className="flex-1 bg-bg">
        <article className="mx-auto w-full max-w-3xl px-6 py-16 sm:py-24">
          <p className="text-[11px] uppercase tracking-[0.18em] text-secondary">
            Legal
          </p>
          <h1 className="mt-4 text-[clamp(2.2rem,5vw,3.4rem)] font-medium leading-[1.05] tracking-[-0.025em] text-fg">
            Privacy
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
                Harbor is an early project. This page is a simple placeholder
                describing how information may be handled when you use the
                service. It will be updated with more detail as the product
                evolves.
              </p>
            </section>

            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                Information you provide
              </h2>
              <p className="mt-3">
                If you create an account or connect a repository, you may
                provide information such as your name, email address, and
                project or repository details needed to run deployments.
              </p>
            </section>

            <section>
              <h2 className="text-[20px] font-medium tracking-[-0.01em] text-fg">
                How it&rsquo;s used
              </h2>
              <p className="mt-3">
                Information is used to operate Harbor: authenticating you,
                managing projects, building and running deployments, and showing
                deployment status and logs.
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
