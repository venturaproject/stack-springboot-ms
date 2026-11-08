import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'

/** Mirrors the backend `NotificationResponse`. */
export interface Notification {
  id: string
  title: string
  message: string
  type: string
  read: boolean
  createdAt: string
}

/** Mirrors the backend `NotificationListResponse`. */
export interface NotificationListResponse {
  data: Notification[]
  unreadCount: number
}

export interface SendNotificationPayload {
  title: string
  message: string
  type?: string
}

export const notificationsApi = {
  list(): Promise<NotificationListResponse> {
    return axios.get(endpoints.notifications.list).then((r) => r.data)
  },
  send(payload: SendNotificationPayload): Promise<Notification> {
    return axios.post(endpoints.notifications.list, payload).then((r) => r.data)
  },
  markRead(id: string): Promise<void> {
    return axios.patch(endpoints.notifications.markRead(id)).then(() => undefined)
  },
  markAllRead(): Promise<void> {
    return axios.patch(endpoints.notifications.markAllRead).then(() => undefined)
  },
  remove(id: string): Promise<void> {
    return axios.delete(endpoints.notifications.detail(id)).then(() => undefined)
  },
  removeAll(): Promise<void> {
    return axios.delete(endpoints.notifications.list).then(() => undefined)
  },
}
