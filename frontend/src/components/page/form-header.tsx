import { ChevronLeft, Eye, Loader2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useI18n } from '@/i18n/context'

/**
 * Header for a create / edit form page: a small back button, the title (+ optional
 * subtitle), and the View / Cancel / Save actions on the right (also rendered as a
 * sticky-feel row on mobile by {@link FormMobileActions}). Matches every form page.
 */
export function FormHeader({
  backTo,
  title,
  subtitle,
  subtitleMono = false,
  viewTo,
  cancelTo,
  submitting,
  submitLabel,
  onSubmit,
}: {
  backTo: string
  title: string
  subtitle?: string
  subtitleMono?: boolean
  viewTo?: string
  cancelTo: string
  submitting: boolean
  submitLabel: string
  onSubmit: () => void
}) {
  const { t } = useI18n()
  const navigate = useNavigate()

  return (
    <div className="flex items-center gap-4">
      <Button variant="outline" size="icon" className="h-7 w-7" onClick={() => navigate(backTo)}>
        <ChevronLeft className="h-4 w-4" />
        <span className="sr-only">{t('common_back')}</span>
      </Button>
      <div className="flex-1">
        <h1 className="whitespace-nowrap text-xl font-semibold tracking-tight">{title}</h1>
        {subtitle && (
          <p className={`text-sm text-muted-foreground${subtitleMono ? ' font-mono' : ''}`}>{subtitle}</p>
        )}
      </div>
      <div className="hidden items-center gap-2 md:ml-auto md:flex">
        {viewTo && (
          <Button variant="outline" onClick={() => navigate(viewTo)} disabled={submitting}>
            <Eye className="mr-2 h-4 w-4" />
            {t('common_view')}
          </Button>
        )}
        <Button variant="outline" onClick={() => navigate(cancelTo)} disabled={submitting}>
          {t('common_cancel')}
        </Button>
        <Button size="sm" onClick={onSubmit} disabled={submitting}>
          {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {submitting ? t('common_saving') : submitLabel}
        </Button>
      </div>
    </div>
  )
}

export function FormMobileActions({
  cancelTo,
  submitting,
  submitLabel,
  onSubmit,
}: {
  cancelTo: string
  submitting: boolean
  submitLabel: string
  onSubmit: () => void
}) {
  const { t } = useI18n()
  const navigate = useNavigate()
  return (
    <div className="flex items-center gap-2 md:hidden">
      <Button variant="outline" className="flex-1" onClick={() => navigate(cancelTo)} disabled={submitting}>
        {t('common_cancel')}
      </Button>
      <Button className="flex-1" onClick={onSubmit} disabled={submitting}>
        {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {submitting ? t('common_saving') : submitLabel}
      </Button>
    </div>
  )
}
