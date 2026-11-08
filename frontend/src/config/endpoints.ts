export const API_ENDPOINTS = {
  users: '/api/users',
  roles: '/api/roles',
  permissions: '/api/permissions',
  products: '/api/products',
  customers: '/api/customers',
  orders: '/api/orders',
  dashboard: '/api/dashboard',
  auth: {
    csrf: '/api/auth/csrf',
    login: '/api/auth/login',
    logout: '/api/auth/logout',
    refresh: '/api/auth/refresh',
    me: '/api/auth/me',
  },
} as const

/** Build a `?page=&size=&search=` query string for a paginated list endpoint. */
export function listQuery(
  options?: { page?: number; size?: number; search?: string } & Record<string, string | number | boolean | undefined>,
): string {
  const params = new URLSearchParams()
  Object.entries(options ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== null) params.set(key, String(value))
  })
  const q = params.toString()
  return q ? `?${q}` : ''
}
