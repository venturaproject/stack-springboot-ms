import { useNavigate } from 'react-router-dom'
import { Users as UsersIcon } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useI18n } from '@/i18n/context'
import { AccessControlTabs } from '@/pages/users/access-control-tabs'
import type { RoleDto } from '@/services/roles-api'

export default function RolesIndex({ roles }: { roles: RoleDto[] }) {
  const { t } = useI18n()
  const navigate = useNavigate()

  return (
    <AuthenticatedLayout title={t('roles_title')}>
      <Main>
        <h1 className="mb-4 flex items-center gap-2 text-2xl font-semibold tracking-tight">
          <UsersIcon className="h-6 w-6" /> {t('access_control_title')}
        </h1>

        <AccessControlTabs />

        <Card>
          <CardHeader>
            <CardTitle>{t('roles_title')}</CardTitle>
            <CardDescription>{roles.length}</CardDescription>
          </CardHeader>
          <CardContent className="px-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('name')}</TableHead>
                  <TableHead>{t('description')}</TableHead>
                  <TableHead>{t('permissions_title')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {roles.map((r) => (
                  <TableRow key={r.id} className="cursor-pointer" onClick={() => navigate(`/admin/roles/${r.id}`)}>
                    <TableCell className="font-medium">{r.name}</TableCell>
                    <TableCell className="text-muted-foreground">{r.description ?? '—'}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{r.permissions.length}</Badge>
                    </TableCell>
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
