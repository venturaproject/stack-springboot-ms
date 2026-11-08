import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi, type Notification, type SendNotificationPayload } from '@/services/notifications-api'

const QUERY_KEY = ['notifications']

export function useNotifications() {
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => notificationsApi.list(),
    staleTime: 30_000,
  })

  const notifications: Notification[] = data?.data ?? []
  const unreadCount = data?.unreadCount ?? 0

  const invalidate = () => qc.invalidateQueries({ queryKey: QUERY_KEY })

  const send = useMutation({
    mutationFn: (payload: SendNotificationPayload) => notificationsApi.send(payload),
    onSuccess: invalidate,
  })
  const markRead = useMutation({ mutationFn: (id: string) => notificationsApi.markRead(id), onSuccess: invalidate })
  const markAllRead = useMutation({ mutationFn: () => notificationsApi.markAllRead(), onSuccess: invalidate })
  const remove = useMutation({ mutationFn: (id: string) => notificationsApi.remove(id), onSuccess: invalidate })
  const removeAll = useMutation({ mutationFn: () => notificationsApi.removeAll(), onSuccess: invalidate })

  return {
    notifications,
    unreadCount,
    isLoading,
    invalidate,
    sendTest: () =>
      send.mutate({
        title: 'Test notification',
        message: 'This is a sample notification created from the UI to preview the feature.',
        type: 'info',
      }),
    isSending: send.isPending,
    markRead: (id: string) => markRead.mutate(id),
    markAllRead: () => markAllRead.mutate(),
    remove: (id: string) => remove.mutate(id),
    removeAll: () => removeAll.mutate(),
  }
}
