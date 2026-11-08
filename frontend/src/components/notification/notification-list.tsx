import type React from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Trash2, CheckCircle, X, BellPlus } from 'lucide-react'
import { useI18n } from '@/i18n/context'
import { useNotifications } from '@/hooks/use-notifications'

const TYPE_CLASSES: Record<string, string> = {
  success: 'bg-green-100 text-green-800 border-green-200',
  warning: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  error: 'bg-red-100 text-red-800 border-red-200',
  info: 'bg-blue-100 text-blue-800 border-blue-200',
}

const NotificationList: React.FC = () => {
  const { t } = useI18n()
  const { notifications, isLoading, sendTest, isSending, markRead, markAllRead, remove, removeAll } = useNotifications()

  if (isLoading) {
    return (
      <div className="flex h-32 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        <Button variant="default" size="sm" onClick={sendTest} disabled={isSending}>
          <BellPlus className="mr-2 h-4 w-4" />
          {t('notification_send_test')}
        </Button>
        {notifications.length > 0 && (
          <>
            <Button
              variant="outline"
              size="sm"
              onClick={markAllRead}
              disabled={notifications.every((n) => n.read)}
            >
              <CheckCircle className="mr-2 h-4 w-4" />
              {t('mark_all_read')}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={removeAll}
              className="text-destructive hover:text-destructive"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              {t('delete_all')}
            </Button>
          </>
        )}
      </div>

      {notifications.length === 0 ? (
        <div className="py-8 text-center text-muted-foreground">{t('no_notifications_yet')}</div>
      ) : (
        notifications.map((n) => (
          <Card key={n.id} className={!n.read ? 'border-l-4 border-l-primary' : undefined}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between">
                <CardTitle className="text-sm">{n.title}</CardTitle>
                <div className="flex gap-2">
                  <Badge variant="secondary" className={`text-xs ${TYPE_CLASSES[n.type] ?? TYPE_CLASSES.info}`}>
                    {n.type.charAt(0).toUpperCase() + n.type.slice(1)}
                  </Badge>
                  {!n.read && (
                    <Button variant="ghost" size="sm" onClick={() => markRead(n.id)} className="h-6 w-6 p-0">
                      <CheckCircle className="h-4 w-4" />
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => remove(n.id)}
                    className="h-6 w-6 p-0 text-destructive hover:text-destructive"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <p className="text-xs text-muted-foreground">{new Date(n.createdAt).toLocaleString()}</p>
            </CardHeader>
            <CardContent>
              <p className="text-sm">{n.message}</p>
            </CardContent>
          </Card>
        ))
      )}
    </div>
  )
}

export default NotificationList
