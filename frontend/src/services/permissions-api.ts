import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'

/** Mirrors the backend `PermissionResponse`. Permissions are read-only reference data. */
export interface PermissionDto {
  id: string
  name: string
  description: string | null
  createdAt: string
}

export const permissionsApi = {
  list(): Promise<PermissionDto[]> {
    return axios.get(endpoints.permissions.list).then((r) => r.data)
  },
}
