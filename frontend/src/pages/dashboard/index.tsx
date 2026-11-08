import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useI18n } from '@/i18n/context'
import { OverviewTab } from './overview-tab'
import { AnalyticsTab } from './analytics-tab'
import { ReportsTab } from './reports-tab'
import { NotificationsTab } from './notifications-tab'
import type { DashboardData } from './types'

export default function Dashboard({ data }: { data: DashboardData }) {
  const { t } = useI18n()

  return (
    <AuthenticatedLayout title={t('dashboard_title')}>
      <Main>
        <div className="mb-2 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <h1 className="text-2xl font-bold tracking-tight">{t('dashboard_title')}</h1>
        </div>

        <Tabs defaultValue="overview" className="space-y-4">
          <div className="w-full overflow-x-auto pb-2">
            <TabsList>
              <TabsTrigger value="overview">{t('overview_tab')}</TabsTrigger>
              <TabsTrigger value="analytics">{t('analytics_tab')}</TabsTrigger>
              <TabsTrigger value="reports">{t('reports_tab')}</TabsTrigger>
              <TabsTrigger value="notifications">{t('notifications_tab')}</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="overview" className="space-y-4">
            <OverviewTab stats={data.stats} recentOrders={data.recentOrders} />
          </TabsContent>
          <TabsContent value="analytics" className="space-y-4">
            <AnalyticsTab
              stats={data.stats}
              productStats={data.productStats}
              customerStats={data.customerStats}
            />
          </TabsContent>
          <TabsContent value="reports" className="space-y-4">
            <ReportsTab restockProducts={data.restockProducts} recentOrders={data.recentOrders} />
          </TabsContent>
          <TabsContent value="notifications" className="space-y-4">
            <div className="max-w-2xl">
              <NotificationsTab />
            </div>
          </TabsContent>
        </Tabs>
      </Main>
    </AuthenticatedLayout>
  )
}
