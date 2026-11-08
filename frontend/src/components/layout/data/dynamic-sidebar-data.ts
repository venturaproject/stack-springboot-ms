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
import { useI18n } from '@/i18n/context'
import { appName } from '@/lib/app-config'

export const DynamicSidebarData = (): SidebarData => {
  const { t } = useI18n()

  return {
    teams: [
      {
        name: appName,
        logo: AppLogo,
        plan: 'Spring + React boilerplate',
      },
    ],
    navGroups: [
      {
        title: t('general'),
        items: [
          { title: t('dashboard'), url: '/admin', icon: IconLayoutDashboard },
          { title: t('products_title'), url: '/admin/products', icon: IconPackage },
          { title: t('customers_title'), url: '/admin/customers', icon: IconAddressBook },
          { title: t('orders_title'), url: '/admin/orders', icon: IconShoppingCart },
        ],
      },
      {
        title: t('access_control'),
        items: [{ title: t('users'), url: '/admin/users', icon: IconUsers }],
      },
      {
        title: t('others'),
        items: [
          {
            title: t('configuration'),
            icon: IconSettings,
            items: [
              { title: t('profile'), url: '/admin/settings', icon: IconUserCog },
              { title: t('appearance'), url: '/admin/settings/appearance', icon: IconPalette },
              { title: t('notifications'), url: '/admin/settings/notifications', icon: IconNotification },
              { title: t('display'), url: '/admin/settings/display', icon: IconBrowserCheck },
            ],
          },
        ],
      },
    ],
  }
}
