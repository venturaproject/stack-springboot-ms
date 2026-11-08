import { ThemeToggle } from "@/components/theme/theme-toggle";
import { Button } from "@/components/ui/button";
import { DocumentTitle } from "@/components/document-title";
import { ArrowRight } from "lucide-react";
import { BrandMark } from "@/components/brand-mark";
import { appName } from "@/lib/app-config";

export const Header = ({ title }: { title?: string }) => {
  return (
    <>
      <DocumentTitle title={title ?? appName}/>
      <header className="container mx-auto flex h-fit items-center justify-between py-4">
      <h1 className="flex items-center gap-3 text-xl font-bold">
        <BrandMark className="size-9 rounded-xl" iconClassName="size-6" />
        <span>{appName}</span>
      </h1>
      <nav className="flex items-center gap-4">
        <a
          target="_blank"
          href="https://github.com/binjuhor/shadcn-admin"
        >
          <Button
            variant="default"
            className="h-fit rounded-full bg-[#222] font-semibold text-white hover:bg-[#222]/90"
          >
            Github
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </a>
        <ThemeToggle />
      </nav>
    </header>
    </>
  );
};
