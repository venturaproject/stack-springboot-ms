import type { DashboardStats } from '@/services/dashboard-api'
import type { Product } from '@/services/products-api'
import type { OrderSummary } from '@/services/orders-api'

export interface ProductStats {
  total: number
  lowStock: number
  outOfStock: number
}

export interface CustomerStats {
  total: number
  withAddress: number
  withoutAddress: number
}

export interface DashboardData {
  stats: DashboardStats
  productStats: ProductStats
  customerStats: CustomerStats
  restockProducts: Product[]
  recentOrders: OrderSummary[]
}

export type { DashboardStats }
