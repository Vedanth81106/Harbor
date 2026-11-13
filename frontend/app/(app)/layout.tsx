import { AppHeader } from "@/components/app/app-header";
import { RequireAuth } from "@/components/app/require-auth";
import { SiteFooter } from "@/components/site-footer";

export default function AppLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <RequireAuth>
      <div className="flex min-h-svh flex-col">
        <AppHeader />
        <main
          id="main"
          tabIndex={-1}
          className="mx-auto w-full max-w-6xl flex-1 px-4 pb-24 pt-10 sm:px-8 sm:pt-14"
        >
          {children}
        </main>
        <SiteFooter />
      </div>
    </RequireAuth>
  );
}
