import {
  IconBrowserCheck,
  IconLayoutDashboard,
  IconPackage,
  IconShoppingCart,
  IconAddressBook,
  IconNotification,
  IconPalette,
  IconSettings,
  IconUserCog,
  IconUsers,
} from '@tabler/icons-react'
import { type SidebarData } from '../types'
import AppLogo from '../app-logo'
import { appName } from '@/lib/app-config'

export const sidebarData: SidebarData = {
  teams: [
    {
      name: appName,
      logo: AppLogo,
      plan: 'Spring + React boilerplate',
    },
  ],
  navGroups: [
    {
      title: 'General',
      items: [
        { title: 'Dashboard', url: '/admin', icon: IconLayoutDashboard },
        { title: 'Products', url: '/admin/products', icon: IconPackage },
        { title: 'Customers', url: '/admin/customers', icon: IconAddressBook },
        { title: 'Orders', url: '/admin/orders', icon: IconShoppingCart },
      ],
    },
    {
      title: 'Access control',
      items: [
        { title: 'Users', url: '/admin/users', icon: IconUsers },
      ],
    },
    {
      title: 'Other',
      items: [
        {
          title: 'Settings',
          icon: IconSettings,
          items: [
            { title: 'Profile', url: '/admin/settings', icon: IconUserCog },
            { title: 'Appearance', url: '/admin/settings/appearance', icon: IconPalette },
            { title: 'Notifications', url: '/admin/settings/notifications', icon: IconNotification },
            { title: 'Display', url: '/admin/settings/display', icon: IconBrowserCheck },
          ],
        },
      ],
    },
  ],
}
