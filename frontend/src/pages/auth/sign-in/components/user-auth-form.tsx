import { HTMLAttributes } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { getErrorMessage } from '@/lib/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/password-input'
import { Label } from '@/components/ui/label'
import InputError from '@/components/InputError'
import { getAuthAdapter } from '@/lib/auth-adapter'
import { useAuthStore } from '@/lib/auth'
import { useI18n } from '@/i18n/context'

type FormData = { login: string; password: string }

type UserAuthFormProps = HTMLAttributes<HTMLDivElement> & {
  status?: string
  canResetPassword?: boolean
}

export function UserAuthForm({ className, status, canResetPassword = true, ...props }: UserAuthFormProps) {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const { t } = useI18n()
  const schema = z.object({
    login: z.string().email(t('sign_in_email_invalid')),
    password: z.string().min(1, t('sign_in_password_required')),
  })

  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } =
    useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { login: '', password: '' } })

  const onSubmit = async (data: FormData) => {
    try {
      const session = await getAuthAdapter().login(data)
      setAuth(session.user, session.permissions, session.roles)
      navigate('/admin')
    } catch (err) {
      const message = getErrorMessage(err, t('invalid_credentials'))
      setError('login', { message })
    }
  }

  return (
    <div className={cn('grid gap-6', className)} {...props}>
      {status && <div className="text-sm font-medium text-green-600">{status}</div>}

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className='grid gap-2'>
          <div className='space-y-1'>
              <Label htmlFor="login">{t('email')}</Label>
            <Input
              id="login"
              type="email"
              placeholder={t('sign_in_email_placeholder')}
              autoComplete="email"
              {...register('login')}
            />
            <InputError message={errors.login?.message} className="mt-2" />
          </div>

          <div className='space-y-1'>
            <div className='flex items-center justify-between'>
              <Label htmlFor="password">{t('password')}</Label>
              {canResetPassword && (
                <Link
                  to='/forgot-password'
                  className='text-sm font-medium text-muted-foreground hover:opacity-75'
                  tabIndex={1}
                >
                  {t('forgot_your_password')}
                </Link>
              )}
            </div>
            <PasswordInput
              id="password"
              placeholder='********'
              autoComplete="current-password"
              {...register('password')}
            />
            <InputError message={errors.password?.message} className="mt-2" />
          </div>

          <Button className='mt-4' disabled={isSubmitting}>
            {isSubmitting ? t('sign_in_submitting') : t('login')}
          </Button>
        </div>
      </form>
    </div>
  )
}
