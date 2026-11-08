// Central API endpoint registry — change the base path here to swap backends.
const API_BASE = '/api'

export const endpoints = {
  auth: {
    login: `${API_BASE}/auth/login`,
    logout: `${API_BASE}/auth/logout`,
    refresh: `${API_BASE}/auth/refresh`,
    me: `${API_BASE}/auth/me`,
    avatar: `${API_BASE}/auth/me/avatar`,
  },
  users: {
    list: `${API_BASE}/users`,
    detail: (id: string | number) => `${API_BASE}/users/${id}`,
    audit: (id: string | number) => `${API_BASE}/users/${id}/audit`,
  },
  roles: {
    list: `${API_BASE}/roles`,
    detail: (id: string | number) => `${API_BASE}/roles/${id}`,
  },
  permissions: {
    list: `${API_BASE}/permissions`,
  },
  products: {
    list: `${API_BASE}/products`,
    detail: (id: string) => `${API_BASE}/products/${id}`,
    stock: (id: string) => `${API_BASE}/products/${id}/stock-adjustments`,
    image: (id: string) => `${API_BASE}/products/${id}/image`,
  },
  customers: {
    list: `${API_BASE}/customers`,
    detail: (id: string) => `${API_BASE}/customers/${id}`,
    address: (id: string) => `${API_BASE}/customers/${id}/address`,
  },
  orders: {
    list: `${API_BASE}/orders`,
    detail: (id: string) => `${API_BASE}/orders/${id}`,
    lines: `${API_BASE}/orders/lines`,
    line: (id: string, lineId: string) => `${API_BASE}/orders/${id}/lines/${lineId}`,
    place: (id: string) => `${API_BASE}/orders/${id}/place`,
    cancel: (id: string) => `${API_BASE}/orders/${id}/cancel`,
  },
  dashboard: `${API_BASE}/dashboard`,
  notifications: {
    list: `${API_BASE}/notifications`,
    detail: (id: string | number) => `${API_BASE}/notifications/${id}`,
    markRead: (id: string | number) => `${API_BASE}/notifications/${id}/read`,
    markAllRead: `${API_BASE}/notifications/read-all`,
  },
  apiClients: {
    list: `${API_BASE}/api-clients`,
    create: `${API_BASE}/api-clients`,
    revoke: (id: string) => `${API_BASE}/api-clients/${id}`,
  },
  extAuth: {
    token: `${API_BASE}/ext/auth/token`,
    refresh: `${API_BASE}/ext/auth/refresh`,
  },
}
