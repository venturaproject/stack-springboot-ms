import { Package, AlertTriangle, Contact, ShoppingCart } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import { useNavigate } from 'react-router-dom'
import { MetricStatCard } from '@/components/metric-stat-card'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from '@/components/ui/chart'
import { useI18n } from '@/i18n/context'
import { formatMoney } from '@/services/products-api'
import type { OrderSummary } from '@/services/orders-api'
import { OrderStatusBadge } from '@/pages/orders/components/order-status-badge'
import type { DashboardStats } from './types'

export function OverviewTab({ stats, recentOrders }: { stats: DashboardStats; recentOrders: OrderSummary[] }) {
  const { t } = useI18n()
  const navigate = useNavigate()

  const ordersByStatus = [
    { name: t('order_status_draft'), value: stats.draftOrders },
    { name: t('order_status_placed'), value: stats.placedOrders },
    { name: t('order_status_cancelled'), value: stats.cancelledOrders },
  ]

  const chartConfig = { value: { label: t('orders_title'), color: '#6366f1' } } satisfies ChartConfig

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div onClick={() => navigate('/admin/products')} className="cursor-pointer">
          <MetricStatCard
            title={t('stat_total_products_title')}
            value={stats.totalProducts}
            subtitle={t('stat_total_products_subtitle')}
            icon={Package}
            sparklineColor="#6366f1"
          />
        </div>
        <div onClick={() => navigate('/admin/products')} className="cursor-pointer">
          <MetricStatCard
            title={t('stat_low_stock_title')}
            value={stats.lowStockProducts}
            subtitle={t('stat_low_stock_subtitle')}
            icon={AlertTriangle}
            sparklineColor="#f59e0b"
          />
        </div>
        <div onClick={() => navigate('/admin/customers')} className="cursor-pointer">
          <MetricStatCard
            title={t('stat_total_customers_title')}
            value={stats.totalCustomers}
            subtitle={t('stat_total_customers_subtitle')}
            icon={Contact}
            sparklineColor="#8b5cf6"
          />
        </div>
        <div onClick={() => navigate('/admin/orders')} className="cursor-pointer">
          <MetricStatCard
            title={t('stat_total_orders_title')}
            value={stats.totalOrders}
            subtitle={t('stat_total_orders_subtitle')}
            icon={ShoppingCart}
            sparklineColor="#06b6d4"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t('analytics_orders_by_status_title')}</CardTitle>
          </CardHeader>
          <CardContent className="h-64 pl-2">
            <ChartContainer config={chartConfig} className="aspect-auto h-full w-full">
              <BarChart data={ordersByStatus}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
                <Bar dataKey="value" fill="var(--color-value)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('reports_recent_orders_title')}</CardTitle>
            <CardDescription>{t('reports_recent_orders_description')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-2">
            {recentOrders.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('orders_empty')}</p>
            ) : (
              recentOrders.map((order) => (
                <div
                  key={order.id}
                  className="flex cursor-pointer items-center justify-between rounded-md border px-3 py-2 hover:bg-muted/50"
                  onClick={() => navigate(`/admin/orders/${order.id}`)}
                >
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-sm">{order.id.slice(0, 8)}</span>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <span className="text-sm text-muted-foreground">
                    {order.totalAmountMinorUnits != null && order.currency
                      ? formatMoney(order.totalAmountMinorUnits, order.currency)
                      : '—'}
                  </span>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
