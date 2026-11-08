import {AppSidebar} from "@/components/layout/app-sidebar"
import {Header} from '@/components/layout/header'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import NotificationButton from '@/components/notification/notification-button'

import {
  SidebarInset,
  SidebarProvider,
} from "@/components/ui/sidebar"
import {ThemeSwitch} from "@/components/theme-switch"
import { type ReactNode, useEffect } from "react"
import { appName } from '@/lib/app-config'

interface AuthenticatedLayoutProps {
  children: ReactNode
  title?: string
  showHeader?: boolean
}

export function AuthenticatedLayout({
    children,
    title,
    showHeader = true,
  }: AuthenticatedLayoutProps) {

  useEffect(() => {
    document.title = title ? `${title} - ${appName}` : appName
  }, [title])

  return (
    <>
      <SidebarProvider>
        <AppSidebar/>
        <SidebarInset>
          {showHeader && <Header>
            <div className='ml-auto flex items-center space-x-2 sm:space-x-4'>
              <Search/>
              <NotificationButton/>
              <ThemeSwitch/>
              <ProfileDropdown/>
            </div>
          </Header>}

          {children}
        </SidebarInset>
      </SidebarProvider>
    </>
  )
}
