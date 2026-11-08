import { useLocation, useNavigate } from 'react-router-dom'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useI18n } from '@/i18n/context'

/** Switches between interactive users and machine clients that consume the external API. */
export function UserAreaTabs() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const selected = pathname.startsWith('/admin/api-clients') ? 'api-clients' : 'users'

  return (
    <Tabs
      value={selected}
      onValueChange={(value) => navigate(value === 'api-clients' ? '/admin/api-clients' : '/admin/users')}
      className="mb-4"
    >
      <TabsList>
        <TabsTrigger value="users">{t('users_title')}</TabsTrigger>
        <TabsTrigger value="api-clients">{t('external_api_users_title')}</TabsTrigger>
      </TabsList>
    </Tabs>
  )
}
