import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'

/** Mirrors the backend `DashboardResponse`. */
export interface DashboardStats {
  totalProducts: number
  lowStockProducts: number
  totalCustomers: number
  totalOrders: number
  draftOrders: number
  placedOrders: number
  cancelledOrders: number
}

export const dashboardApi = {
  stats(): Promise<DashboardStats> {
    return axios.get(endpoints.dashboard).then((r) => r.data)
  },
}
