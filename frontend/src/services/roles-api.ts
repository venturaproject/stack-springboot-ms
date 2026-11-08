import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'

/** Mirrors the backend `RoleResponse`. Roles are read-only in this boilerplate. */
export interface RoleDto {
  id: string
  name: string
  description: string | null
  permissions: string[]
  createdAt: string
}

export const rolesApi = {
  list(): Promise<RoleDto[]> {
    return axios.get(endpoints.roles.list).then((r) => r.data)
  },
  detail(id: string): Promise<RoleDto> {
    return axios.get(endpoints.roles.detail(id)).then((r) => r.data)
  },
}
