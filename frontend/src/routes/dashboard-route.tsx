import { useQuery } from '@tanstack/react-query'
import { RoutePending } from '@/components/route-pending'
import { dashboardApi, type DashboardStats } from '@/services/dashboard-api'
import { productsApi } from '@/services/products-api'
import { customersApi } from '@/services/customers-api'
import { ordersApi } from '@/services/orders-api'
import Dashboard from '@/pages/dashboard'
import type { DashboardData } from '@/pages/dashboard/types'

const EMPTY_STATS: DashboardStats = {
  totalProducts: 0,
  lowStockProducts: 0,
  totalCustomers: 0,
  totalOrders: 0,
  draftOrders: 0,
  placedOrders: 0,
  cancelledOrders: 0,
}

const LOW_STOCK = 5

export default function DashboardRoute() {
  const statsQuery = useQuery({ queryKey: ['dashboard'], queryFn: () => dashboardApi.stats() })
  const productsQuery = useQuery({
    queryKey: ['products', { size: 200 }],
    queryFn: () => productsApi.list({ size: 200 }),
  })
  const customersQuery = useQuery({
    queryKey: ['customers', { size: 1 }],
    queryFn: () => customersApi.list({ size: 1 }),
  })
  const ordersQuery = useQuery({
    queryKey: ['orders', { size: 5 }],
    queryFn: () => ordersApi.list({ size: 5 }),
  })

  if (statsQuery.isLoading || productsQuery.isLoading || customersQuery.isLoading || ordersQuery.isLoading) {
    return <RoutePending />
  }

  const products = productsQuery.data?.data ?? []
  const restockProducts = products
    .filter((p) => p.stockQuantity <= LOW_STOCK)
    .sort((a, b) => a.stockQuantity - b.stockQuantity)

  const data: DashboardData = {
    stats: statsQuery.data ?? EMPTY_STATS,
    productStats: {
      total: products.length,
      lowStock: restockProducts.length,
      outOfStock: products.filter((p) => p.stockQuantity === 0).length,
    },
    customerStats: customersQuery.data?.stats ?? { total: 0, withAddress: 0, withoutAddress: 0 },
    restockProducts,
    recentOrders: ordersQuery.data?.data ?? [],
  }

  return <Dashboard data={data} />
}
