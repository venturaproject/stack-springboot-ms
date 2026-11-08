import { Users as UsersIcon } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { useI18n } from '@/i18n/context'
import { UserAreaTabs } from '@/pages/users/user-area-tabs'
import { ApiClientsPanel } from './api-clients-panel'

export default function ApiClientsIndex() {
  const { t } = useI18n()

  return (
    <AuthenticatedLayout title={t('api_clients_title')}>
      <Main>
        <h1 className="mb-4 flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <UsersIcon className="h-6 w-6" /> {t('access_control_title')}
        </h1>

        <UserAreaTabs />

        <ApiClientsPanel />
      </Main>
    </AuthenticatedLayout>
  )
}
