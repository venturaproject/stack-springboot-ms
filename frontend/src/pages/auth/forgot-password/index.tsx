import { Link } from "react-router-dom"
import { Card } from '@/components/ui/card'
import AuthLayout from '../auth-layout'
import { ForgotForm } from './components/forgot-password-form'
import { useI18n } from '@/i18n/context'

export default function ForgotPassword() {
  const { t } = useI18n()
  return (
    <AuthLayout title={t('forgot_password_title')}>
      <Card className='p-6'>
        <div className='mb-2 flex flex-col space-y-2 text-left'>
          <h1 className='text-md font-semibold tracking-tight'>
            {t('forgot_password_title')}
          </h1>
          <p className='text-sm text-muted-foreground'>
            {t('forgot_password_description')}
          </p>
        </div>
        <ForgotForm />
        <p className='mt-4 px-8 text-center text-sm text-muted-foreground'>
          {t('remembered_password')}{' '}
          <Link
            to='/login'
            className='underline underline-offset-4 hover:text-primary'
          >
            {t('back_to_login')}
          </Link>
          .
        </p>
      </Card>
    </AuthLayout>
  )
}
