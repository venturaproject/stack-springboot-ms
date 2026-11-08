import { useNavigate } from 'react-router-dom'
import { Pencil, Shield, ClipboardList } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { DetailHeader } from '@/components/page/detail-header'
import { useI18n } from '@/i18n/context'
import { formatDate } from '@/lib/format'
import type { UserDto } from '@/services/users-api'
import { AuditLogTab } from './components/audit-log-tab'
import { UserStatusBadge } from './components/user-status-badge'

function initials(user: UserDto): string {
  return ((user.firstName[0] ?? '') + (user.lastName[0] ?? '')).toUpperCase() || 'U'
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="mb-1 text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <div className="text-sm font-medium">{children}</div>
    </div>
  )
}

export default function UserShow({ user }: { user: UserDto }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const name = `${user.firstName} ${user.lastName}`.trim() || user.email

  return (
    <AuthenticatedLayout title={name}>
      <Main>
        <div className="mx-auto grid max-w-5xl flex-1 items-start gap-6 md:gap-8">
          <DetailHeader backTo="/admin/users" title={name} subtitle={user.email}>
            <Button onClick={() => navigate(`/admin/users/${user.id}/edit`)}>
              <Pencil className="mr-2 h-4 w-4" />
              {t('common_edit')}
            </Button>
          </DetailHeader>

          <Card>
            <CardHeader>
              <CardTitle>{t('users_show_profile_title')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-start gap-6">
                <Avatar className="h-20 w-20 shrink-0">
                  {user.avatarUrl && <AvatarImage src={user.avatarUrl} alt={name} />}
                  <AvatarFallback className="bg-primary/10 text-2xl font-bold text-primary">
                    {initials(user)}
                  </AvatarFallback>
                </Avatar>
                <div className="grid flex-1 grid-cols-2 gap-4">
                  <Field label={t('users_show_full_name_label')}>{name}</Field>
                  <Field label={t('email')}>{user.email}</Field>
                  <Field label={t('common_status')}>
                    <UserStatusBadge active={user.active} />
                  </Field>
                  <Field label={t('common_created_at')}>{formatDate(user.createdAt)}</Field>
                </div>
              </div>
            </CardContent>
          </Card>

          <Tabs defaultValue="roles">
            <TabsList>
              <TabsTrigger value="roles" className="gap-2">
                <Shield className="h-4 w-4" />
                {t('roles_title')}
              </TabsTrigger>
              <TabsTrigger value="audit" className="gap-2">
                <ClipboardList className="h-4 w-4" />
                {t('users_tab_audit_log')}
              </TabsTrigger>
            </TabsList>

            <TabsContent value="roles" className="mt-4">
              <Card>
                <CardContent className="pt-6">
                  {user.roles.length === 0 ? (
                    <p className="text-sm text-muted-foreground">{t('users_no_roles_assigned')}</p>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {user.roles.map((r) => (
                        <Badge key={r} variant="secondary">
                          {r}
                        </Badge>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="audit" className="mt-4">
              <Card>
                <CardContent className="pt-6">
                  <AuditLogTab userId={user.id} />
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
