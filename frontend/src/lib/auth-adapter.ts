import axios from 'axios'
import { API_ENDPOINTS } from '@/config/endpoints'
import { PUBLIC_API_URL } from '@/config/env'
import type { AuthUser } from './auth'

export interface SessionData {
  user: AuthUser
  permissions: string[]
  roles: string[]
}

export interface AuthAdapter {
  /** Headers for protected requests. Spring auth currently uses HttpOnly cookies. */
  getRequestHeaders(): Record<string, string>
  login(credentials: Record<string, unknown>): Promise<SessionData>
  logout(): Promise<void>
  fetchSession(): Promise<SessionData | null>
  refresh(): Promise<boolean>
  onUnauthorized(): void
}

interface SpringUserResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  avatarUrl?: string | null
  roles?: string[]
  permissions?: string[]
}

const http = axios.create({
  baseURL: PUBLIC_API_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

let csrfToken: string | null = null
let csrfTokenRequest: Promise<void> | null = null

function csrfHeaders(): Record<string, string> {
  return csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}
}

http.interceptors.request.use((config) => {
  if (config.method && ['post', 'put', 'patch', 'delete'].includes(config.method)) {
    Object.assign(config.headers, csrfHeaders())
  }
  return config
})

async function ensureCsrfToken(): Promise<void> {
  if (csrfToken) return
  csrfTokenRequest ??= http
    .get<{ token: string }>(API_ENDPOINTS.auth.csrf)
    .then(({ data }) => {
      csrfToken = data.token
    })
    .catch((error: unknown) => {
      csrfTokenRequest = null
      throw error
    })
  await csrfTokenRequest
}

function mapUser(raw: SpringUserResponse): AuthUser {
  return {
    id: raw.id,
    name: `${raw.firstName ?? ''} ${raw.lastName ?? ''}`.trim() || raw.email,
    email: raw.email,
    avatar: raw.avatarUrl ?? null,
    roles: raw.roles ?? [],
    permissions: raw.permissions ?? [],
  }
}

function toSession(raw: SpringUserResponse): SessionData {
  const user = mapUser(raw)
  return { user, permissions: user.permissions ?? [], roles: user.roles ?? [] }
}

/** Adapter for the Spring monolith's HttpOnly-cookie authentication API. */
export class SpringHttpOnlyAdapter implements AuthAdapter {
  getRequestHeaders(): Record<string, string> {
    return csrfHeaders()
  }

  async login(credentials: Record<string, unknown>): Promise<SessionData> {
    await ensureCsrfToken()
    const { data } = await http.post<{ user: SpringUserResponse }>(API_ENDPOINTS.auth.login, {
      email: credentials.email ?? credentials.login,
      password: credentials.password,
    })
    return toSession(data.user)
  }

  async logout(): Promise<void> {
    await ensureCsrfToken()
    await http.post(API_ENDPOINTS.auth.logout)
  }

  async fetchSession(): Promise<SessionData | null> {
    try {
      await ensureCsrfToken()
      const { data } = await http.get<SpringUserResponse>(API_ENDPOINTS.auth.me)
      return toSession(data)
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) return null
      throw error
    }
  }

  async refresh(): Promise<boolean> {
    try {
      await ensureCsrfToken()
      await http.post(API_ENDPOINTS.auth.refresh)
      return true
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) return false
      throw error
    }
  }

  onUnauthorized(): void {
    window.location.assign('/login')
  }
}

let adapter: AuthAdapter = new SpringHttpOnlyAdapter()

export const getAuthAdapter = (): AuthAdapter => adapter

/** Override once at application startup when connecting a different backend. */
export const setAuthAdapter = (nextAdapter: AuthAdapter): void => {
  adapter = nextAdapter
}
