import {Link} from "react-router-dom";
import { useI18n } from '@/i18n/context'

export function TermPrivacyLink({termLink='', privacyLink = ''}) {
  const { t } = useI18n()
  return (
    <>
      <p className='mt-4 px-8 text-center text-sm text-muted-foreground'>
        {t('by_clicking_continue')}{' '}
        <Link
          to={termLink}
          className='underline underline-offset-4 hover:text-primary'
        >
          {t('terms_of_service')}
        </Link>{' '}
        and{' '}
        <Link
          to={privacyLink}
          className='underline underline-offset-4 hover:text-primary'
        >
          {t('privacy_policy')}
        </Link>
        .
      </p>
    </>
  )
}
