import { useLocation, useNavigate } from 'react-router-dom'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useI18n } from '@/i18n/context'

/**
 * Shared sub-navigation for the Access Control area. The sidebar only links to Users —
 * Roles, Permissions and API clients are reached from here instead, as tabs within the
 * same section rather than separate top-level nav entries.
 */
export function AccessControlTabs({ hideUserSections = false }: { hideUserSections?: boolean }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const current = pathname.startsWith('/admin/roles')
    ? 'roles'
    : pathname.startsWith('/admin/permissions')
      ? 'permissions'
      : pathname.startsWith('/admin/api-clients')
        ? 'api-clients'
        : 'users'
  const selected = hideUserSections && (current === 'users' || current === 'api-clients') ? undefined : current

  const change = (value: string) => {
    if (value === current) return
    navigate(`/admin/${value}`)
  }

  return (
    <Tabs value={selected} onValueChange={change} className="mb-4">
      <TabsList>
        {!hideUserSections && <TabsTrigger value="users">{t('users_title')}</TabsTrigger>}
        <TabsTrigger value="roles">{t('roles_title')}</TabsTrigger>
        <TabsTrigger value="permissions">{t('permissions_title')}</TabsTrigger>
        {!hideUserSections && <TabsTrigger value="api-clients">{t('api_clients_title')}</TabsTrigger>}
      </TabsList>
    </Tabs>
  )
}
