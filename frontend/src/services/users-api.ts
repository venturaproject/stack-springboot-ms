import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'
import { listQuery } from '@/config/endpoints'
import type { Paginated } from '@/lib/api-utils'

/** Mirrors the backend `AuditLogResponse`. */
export interface AuditEntry {
  id: string
  actorId: string
  action: string
  entity: string
  entityId: string | null
  changes: string | null
  createdAt: string
}

/** Mirrors the backend `UserResponse`. */
export interface UserDto {
  id: string
  email: string
  firstName: string
  lastName: string
  avatarUrl: string | null
  active: boolean
  roles: string[]
  createdAt: string
  updatedAt: string | null
}

export interface CreateUserPayload {
  email: string
  password: string
  firstName: string
  lastName: string
  roleIds: string[]
}

export interface UpdateUserPayload {
  firstName: string
  lastName: string
  active: boolean
  roleIds: string[]
}

export const usersApi = {
  list(params?: { page?: number; size?: number; search?: string }): Promise<Paginated<UserDto>> {
    return axios.get(endpoints.users.list + listQuery(params)).then((r) => r.data)
  },

  detail(id: string): Promise<UserDto> {
    return axios.get(endpoints.users.detail(id)).then((r) => r.data)
  },

  create(payload: CreateUserPayload): Promise<UserDto> {
    return axios.post(endpoints.users.list, payload).then((r) => r.data)
  },

  update(id: string, payload: UpdateUserPayload): Promise<UserDto> {
    return axios.put(endpoints.users.detail(id), payload).then((r) => r.data)
  },

  remove(id: string): Promise<void> {
    return axios.delete(endpoints.users.detail(id)).then(() => undefined)
  },

  auditLog(id: string, params?: { page?: number; size?: number }): Promise<Paginated<AuditEntry>> {
    return axios.get(endpoints.users.audit(id) + listQuery(params)).then((r) => r.data)
  },
}
