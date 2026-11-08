import { SidebarMenu, SidebarMenuItem } from '@/components/ui/sidebar';
import { appName } from '@/lib/app-config'

const currentYear = new Date().getFullYear()

export function CopyrightFooter() {
  return (
    <SidebarMenu>
      <SidebarMenuItem className="justify-center text-center py-2">
        <div className="text-xs text-muted-foreground">
          {appName} © {currentYear}
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
