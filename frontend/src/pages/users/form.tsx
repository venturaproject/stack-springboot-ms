import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { KeyRound, Shield, ClipboardList } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { getErrorMessage } from '@/lib/errors'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import InputError from '@/components/InputError'
import { generatePassword } from '@/lib/generate-password'
import { formatDate } from '@/lib/format'
import { useI18n } from '@/i18n/context'
import { usersApi, type UserDto } from '@/services/users-api'
import type { RoleDto } from '@/services/roles-api'
import { AuditLogTab } from './components/audit-log-tab'

const schema = z.object({
  email: z.string().email(),
  password: z.string().optional(),
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  active: z.boolean(),
  roleIds: z.array(z.string()),
})
type FormData = z.infer<typeof schema>

function RoleChecklist({
  roles,
  selected,
  onToggle,
}: {
  roles: RoleDto[]
  selected: string[]
  onToggle: (id: string, checked: boolean) => void
}) {
  return (
    <div className="space-y-1">
      {roles.map((r) => (
        <label
          key={r.id}
          className="flex cursor-pointer items-center gap-3 rounded-md px-2 py-1.5 transition-colors hover:bg-muted/40"
        >
          <Checkbox checked={selected.includes(r.id)} onCheckedChange={(c) => onToggle(r.id, !!c)} />
          <span className="flex-1 text-sm font-medium">{r.name}</span>
          {r.description && <span className="text-xs text-muted-foreground">{r.description}</span>}
        </label>
      ))}
    </div>
  )
}

interface Props {
  user?: UserDto
  roles: RoleDto[]
}

export default function UserForm({ user, roles }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const isEdit = !!user

  const initialRoleIds = user ? roles.filter((r) => user.roles.includes(r.name)).map((r) => r.id) : []

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: user?.email ?? '',
      password: '',
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      active: user?.active ?? true,
      roleIds: initialRoleIds,
    },
  })

  const roleIds = useWatch({ control, name: 'roleIds' })
  const active = useWatch({ control, name: 'active' })
  const toggleRole = (id: string, checked: boolean) =>
    setValue('roleIds', checked ? [...roleIds, id] : roleIds.filter((r) => r !== id))

  const onSubmit = async (data: FormData) => {
    try {
      if (isEdit) {
        await usersApi.update(user.id, {
          firstName: data.firstName,
          lastName: data.lastName,
          active: data.active,
          roleIds: data.roleIds,
        })
        toast.success(t('user_update_success'))
      } else {
        if (!data.password || data.password.length < 8) {
          setError('password', { message: t('user_password_min') })
          return
        }
        await usersApi.create({
          email: data.email,
          password: data.password,
          firstName: data.firstName,
          lastName: data.lastName,
          roleIds: data.roleIds,
        })
        toast.success(t('user_create_success'))
      }
      qc.invalidateQueries({ queryKey: ['users'] })
      navigate('/admin/users')
    } catch (err) {
      const message = getErrorMessage(err, t('user_save_error'))
      setError('email', { message })
    }
  }

  const submit = handleSubmit(onSubmit)
  const title = isEdit ? t('users_edit_heading') : t('add_new_user')

  return (
    <AuthenticatedLayout title={title}>
      <Main>
        <div className="grid max-w-4xl flex-1 auto-rows-max items-start gap-4 md:gap-6">
          <FormHeader
            backTo="/admin/users"
            title={title}
            viewTo={isEdit ? `/admin/users/${user.id}` : undefined}
            cancelTo="/admin/users"
            submitting={isSubmitting}
            submitLabel={isEdit ? t('common_save_changes') : t('create_user')}
            onSubmit={submit}
          />

          <div className="grid gap-4 md:grid-cols-2 lg:gap-6">
            <Card className="h-full">
              <CardHeader>
                <CardTitle>{t('users_edit_details_title')}</CardTitle>
                <CardDescription>{t('update_user_description')}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="grid gap-2">
                    <Label htmlFor="firstName">{t('user_field_first_name')}</Label>
                    <Input id="firstName" {...register('firstName')} />
                    <InputError message={errors.firstName?.message} />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="lastName">{t('user_field_last_name')}</Label>
                    <Input id="lastName" {...register('lastName')} />
                    <InputError message={errors.lastName?.message} />
                  </div>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="email">{t('email')}</Label>
                  <Input id="email" type="email" disabled={isEdit} {...register('email')} />
                  <InputError message={errors.email?.message} />
                </div>
              </CardContent>
            </Card>

            <Card className="h-full">
              <CardContent className="grid gap-5 pt-6">
                <div className="grid gap-2">
                  <Label>{t('roles_title')}</Label>
                  <RoleChecklist roles={roles} selected={roleIds} onToggle={toggleRole} />
                </div>

                {!isEdit && (
                  <div className="grid gap-2">
                    <Label htmlFor="password">{t('password')}</Label>
                    <div className="flex gap-2">
                      <Input id="password" type="text" className="font-mono" {...register('password')} />
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        onClick={() => setValue('password', generatePassword())}
                      >
                        <KeyRound className="h-4 w-4" />
                      </Button>
                    </div>
                    <InputError message={errors.password?.message} />
                  </div>
                )}

                {isEdit && (
                  <>
                    <label className="flex items-center gap-2 text-sm">
                      <Checkbox checked={active} onCheckedChange={(c) => setValue('active', !!c)} />
                      {t('common_active')}
                    </label>
                    <div className="grid gap-3 border-t pt-4 text-sm">
                      <div>
                        <p className="text-xs text-muted-foreground">{t('users_edit_user_id_label')}</p>
                        <p className="mt-0.5 break-all font-mono text-xs">{user.id}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">{t('common_created_at')}</p>
                        <p className="mt-0.5 font-medium">{formatDate(user.createdAt)}</p>
                      </div>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          {isEdit && (
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
                    <RoleChecklist roles={roles} selected={roleIds} onToggle={toggleRole} />
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
          )}

          <FormMobileActions
            cancelTo="/admin/users"
            submitting={isSubmitting}
            submitLabel={isEdit ? t('common_save_changes') : t('create_user')}
            onSubmit={submit}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
