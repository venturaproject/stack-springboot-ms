import { Bell } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import NotificationList from '@/components/notification/notification-list'
import { useI18n } from '@/i18n/context'

export function NotificationsTab() {
  const { t } = useI18n()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bell className="h-5 w-5" />
          {t('notifications_tab')}
        </CardTitle>
        <CardDescription>{t('dashboard_notifications_description')}</CardDescription>
      </CardHeader>
      <CardContent>
        <NotificationList />
      </CardContent>
    </Card>
  )
}
