import { Users as UsersIcon } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useI18n } from '@/i18n/context'
import { AccessControlTabs } from '@/pages/users/access-control-tabs'
import type { PermissionDto } from '@/services/permissions-api'

export default function PermissionsIndex({ permissions }: { permissions: PermissionDto[] }) {
  const { t } = useI18n()

  return (
    <AuthenticatedLayout title={t('permissions_title')}>
      <Main>
        <h1 className="mb-4 flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <UsersIcon className="h-6 w-6" /> {t('access_control_title')}
        </h1>

        <AccessControlTabs />

        <Card>
          <CardHeader>
            <CardTitle>{t('permissions_title')}</CardTitle>
            <CardDescription>{permissions.length}</CardDescription>
          </CardHeader>
          <CardContent className="px-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('name')}</TableHead>
                  <TableHead>{t('description')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {permissions.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="font-mono text-sm">{p.name}</TableCell>
                    <TableCell className="text-muted-foreground">{p.description ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </Main>
    </AuthenticatedLayout>
  )
}
