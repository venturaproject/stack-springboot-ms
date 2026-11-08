import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { useI18n } from '@/i18n/context'

/** Same palette as the reference's `callTypes` map. */
const STATUS_STYLES: Record<'active' | 'inactive', string> = {
  active: 'bg-teal-100/40 text-teal-900 dark:text-teal-200 border-teal-200',
  inactive: 'bg-neutral-300/40 text-neutral-700 dark:text-neutral-300 border-neutral-300',
}

export function UserStatusBadge({ active }: { active: boolean }) {
  const { t } = useI18n()
  const key = active ? 'active' : 'inactive'
  return (
    <Badge variant="outline" className={cn('rounded-full capitalize', STATUS_STYLES[key])}>
      {active ? t('common_active') : t('common_inactive')}
    </Badge>
  )
}
