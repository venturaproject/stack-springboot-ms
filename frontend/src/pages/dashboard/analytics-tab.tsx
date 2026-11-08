import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from '@/components/ui/chart'
import { useI18n } from '@/i18n/context'
import type { CustomerStats, DashboardStats, ProductStats } from './types'

const STOCK_COLORS = ['#10b981', '#f59e0b', '#ef4444']
const ADDRESS_COLORS = ['#6366f1', '#94a3b8']

interface Props {
  stats: DashboardStats
  productStats: ProductStats
  customerStats: CustomerStats
}

export function AnalyticsTab({ stats, productStats, customerStats }: Props) {
  const { t } = useI18n()

  const ordersByStatus = [
    { name: t('order_status_draft'), value: stats.draftOrders },
    { name: t('order_status_placed'), value: stats.placedOrders },
    { name: t('order_status_cancelled'), value: stats.cancelledOrders },
  ]

  const healthyStock = Math.max(productStats.total - productStats.lowStock, 0)
  const lowButInStock = Math.max(productStats.lowStock - productStats.outOfStock, 0)
  const stockHealth = [
    { name: t('analytics_stock_healthy'), value: healthyStock },
    { name: t('analytics_stock_low'), value: lowButInStock },
    { name: t('analytics_stock_out'), value: productStats.outOfStock },
  ]

  const addressCoverage = [
    { name: t('stat_with_address_title'), value: customerStats.withAddress },
    { name: t('stat_without_address_title'), value: customerStats.withoutAddress },
  ]

  const ordersChartConfig = { value: { label: t('orders_title'), color: '#6366f1' } } satisfies ChartConfig
  const pieConfig = { value: { label: t('analytics_stock_health_title') } } satisfies ChartConfig

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>{t('analytics_orders_by_status_title')}</CardTitle>
          <CardDescription>{t('analytics_orders_by_status_description')}</CardDescription>
        </CardHeader>
        <CardContent className="h-72">
          <ChartContainer config={ordersChartConfig} className="aspect-auto h-full w-full">
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
          <CardTitle>{t('analytics_stock_health_title')}</CardTitle>
          <CardDescription>{t('analytics_stock_health_description')}</CardDescription>
        </CardHeader>
        <CardContent className="h-72">
          <ChartContainer config={pieConfig} className="aspect-auto h-full w-full">
            <PieChart>
              <Pie data={stockHealth} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                {stockHealth.map((entry, index) => (
                  <Cell key={entry.name} fill={STOCK_COLORS[index % STOCK_COLORS.length]} />
                ))}
              </Pie>
              <ChartTooltip content={<ChartTooltipContent hideLabel />} />
              <Legend />
            </PieChart>
          </ChartContainer>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t('analytics_address_coverage_title')}</CardTitle>
          <CardDescription>{t('analytics_address_coverage_description')}</CardDescription>
        </CardHeader>
        <CardContent className="h-72">
          <ChartContainer config={pieConfig} className="aspect-auto h-full w-full">
            <PieChart>
              <Pie data={addressCoverage} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                {addressCoverage.map((entry, index) => (
                  <Cell key={entry.name} fill={ADDRESS_COLORS[index % ADDRESS_COLORS.length]} />
                ))}
              </Pie>
              <ChartTooltip content={<ChartTooltipContent hideLabel />} />
              <Legend />
            </PieChart>
          </ChartContainer>
        </CardContent>
      </Card>
    </div>
  )
}
