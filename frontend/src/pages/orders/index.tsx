import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import { useNavigate } from 'react-router-dom'
import { Plus, Download } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { DropdownMenuItem } from '@/components/ui/dropdown-menu'
import {
  BulkSelectionBar,
  DataTable,
  DataTablePagination,
  DataTableViewOptions,
} from '@/components/data-table'
import { useBulkSelection } from '@/hooks/use-bulk-selection'
import { useColumnReorder } from '@/hooks/use-column-reorder'
import { useTableFilters } from '@/hooks/use-table-filters'
import { emptyPage, type Paginated } from '@/lib/api-utils'
import { downloadCsv } from '@/lib/csv'
import { useI18n } from '@/i18n/context'
import { formatMoney } from '@/services/products-api'
import type { OrderStatus, OrderSummary } from '@/services/orders-api'
import { buildOrdersColumns, orderColumnLabels } from './columns'

const TABS: (OrderStatus | 'all')[] = ['all', 'DRAFT', 'PLACED', 'CONFIRMED', 'CANCELLED']

interface OrderFilters {
  status?: string
  [key: string]: string | undefined
}

interface Props {
  page?: Paginated<OrderSummary>
  filters?: OrderFilters
}

export default function OrdersIndex({ page = emptyPage<OrderSummary>(), filters: initial = {} }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const bulk = useBulkSelection(page.data.map((o) => o.id), page.count)

  const { filters, navigate: navigateFilters, handlePageChange, handleSizeChange } = useTableFilters<OrderFilters>({
    basePath: '/admin/orders',
    initialFilters: initial,
    initialSize: page.perPage,
    onNavigate: bulk.clearSelection,
  })

  const currentTab = filters.status ?? 'all'
  const changeTab = (value: string) => {
    const next = { ...filters }
    if (value === 'all') delete next.status
    else next.status = value
    navigateFilters(next)
  }

  const columns = buildOrdersColumns({
    t,
    onView: (id) => navigate(`/admin/orders/${id}`),
    selectedIds: bulk.selectedIds,
    allPageSelected: bulk.allPageSelected,
    toggleSelectAll: bulk.toggleSelectAll,
    toggleSelectRow: bulk.toggleSelectRow,
  })

  const { columnOrder, columnVisibility, setColumnVisibility, handleDragStart, handleDrop } = useColumnReorder(
    columns.map((c) => c.id as string),
  )

  // TanStack Table's stable instance API is intentionally skipped by React Compiler.
  // oxlint-disable-next-line react/incompatible-library
  const table = useReactTable({
    data: page.data,
    columns,
    state: { columnOrder, columnVisibility },
    onColumnVisibilityChange: setColumnVisibility,
    getCoreRowModel: getCoreRowModel(),
  })

  const exportSelected = () => {
    const rows = page.data.filter((o) => bulk.selectedIds.has(o.id))
    downloadCsv('orders.csv', rows, [
      { header: 'Order', value: (o) => o.id },
      { header: 'Status', value: (o) => o.status },
      {
        header: 'Total',
        value: (o) => (o.totalAmountMinorUnits != null && o.currency ? formatMoney(o.totalAmountMinorUnits, o.currency) : ''),
      },
      { header: 'Created', value: (o) => o.createdAt },
    ])
  }

  return (
    <AuthenticatedLayout title={t('orders_title')}>
      <Main>
        <div className="grid flex-1 items-start gap-4 md:gap-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-2xl font-bold tracking-tight">{t('orders_title')}</h2>
              <p className="text-muted-foreground">{t('orders_page_description')}</p>
            </div>
            <Button onClick={() => navigate('/admin/orders/create')} className="w-fit">
              <Plus className="mr-2 h-4 w-4" />
              {t('orders_new_button')}
            </Button>
          </div>

          <BulkSelectionBar
            visible={bulk.selectedIds.size > 0}
            selectedCount={bulk.selectedCount}
            total={page.count}
            allPageSelected={bulk.allPageSelected}
            selectAllRecords={bulk.selectAllRecords}
            onSelectAllRecords={() => bulk.setSelectAllRecords(true)}
            onSelectPageOnly={() => bulk.setSelectAllRecords(false)}
            onClearSelection={bulk.clearSelection}
            countLabel={t('orders_selected_count', { count: bulk.selectedCount })}
            actions={
              <DropdownMenuItem onClick={exportSelected}>
                <Download className="mr-2 h-4 w-4" />
                {t('common_export_csv')}
              </DropdownMenuItem>
            }
          />

          <Tabs value={currentTab} onValueChange={changeTab} className="space-y-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
              <TabsList>
                {TABS.map((tab) => (
                  <TabsTrigger key={tab} value={tab}>
                    {tab === 'all'
                      ? t('products_tab_all')
                      : t(`order_status_${tab.toLowerCase()}`)}
                  </TabsTrigger>
                ))}
              </TabsList>
              <div className="lg:ml-auto">
                <DataTableViewOptions table={table} columnLabels={orderColumnLabels(t)} />
              </div>
            </div>

            <Card>
              <CardHeader>
                <CardTitle>{t('orders_title')}</CardTitle>
                <CardDescription>{t('page_of', { current: page.page, total: page.totalPages })}</CardDescription>
              </CardHeader>
              <CardContent className="px-0">
                <DataTable
                  table={table}
                  colCount={columns.length}
                  emptyMessage={t('orders_empty')}
                  onDragStart={handleDragStart}
                  onDrop={handleDrop}
                />
              </CardContent>
              <DataTablePagination
                page={page.page}
                totalPages={page.totalPages}
                perPage={page.perPage}
                count={page.count}
                selectedCount={bulk.selectedCount}
                onPageChange={handlePageChange}
                onPerPageChange={handleSizeChange}
              />
            </Card>
          </Tabs>
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
