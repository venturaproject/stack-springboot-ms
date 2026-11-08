import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import { useNavigate } from 'react-router-dom'
import { Contact, MapPin, MapPinOff, Plus, Search, X, Download } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { MetricStatCard } from '@/components/metric-stat-card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { downloadCsv } from '@/lib/csv'
import { useI18n } from '@/i18n/context'
import type { CustomerListPage } from '@/services/customers-api'
import { buildCustomersColumns, customerColumnLabels } from './columns'

const EMPTY: CustomerListPage = {
  data: [],
  page: 1,
  perPage: 20,
  count: 0,
  totalPages: 1,
  stats: { total: 0, withAddress: 0, withoutAddress: 0 },
}

interface CustomerFilters {
  search?: string
  hasAddress?: string
  [key: string]: string | undefined
}

interface Props {
  page?: CustomerListPage
  filters?: CustomerFilters
}

export default function CustomersIndex({ page = EMPTY, filters: initial = {} }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const bulk = useBulkSelection(page.data.map((c) => c.id), page.count)

  const { filters, searchTerm, navigate: navigateFilters, handleSearch, handlePageChange, handleSizeChange } =
    useTableFilters<CustomerFilters>({
      basePath: '/admin/customers',
      initialFilters: initial,
      initialSize: page.perPage,
      onNavigate: bulk.clearSelection,
    })

  const currentTab = filters.hasAddress ?? 'all'
  const changeTab = (value: string) => {
    const next = { ...filters }
    if (value === 'all') delete next.hasAddress
    else next.hasAddress = value
    navigateFilters(next)
  }

  const columns = buildCustomersColumns({
    t,
    onView: (id) => navigate(`/admin/customers/${id}`),
    onNewOrder: () => navigate('/admin/orders/create'),
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
    const rows = page.data.filter((c) => bulk.selectedIds.has(c.id))
    downloadCsv('customers.csv', rows, [
      { header: 'Name', value: (c) => c.name },
      { header: 'Email', value: (c) => c.email },
      { header: 'City', value: (c) => c.address?.city ?? '' },
      { header: 'Country', value: (c) => c.address?.country ?? '' },
    ])
  }

  return (
    <AuthenticatedLayout title={t('customers_title')}>
      <Main>
        <div className="grid flex-1 items-start gap-4 md:gap-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-2xl font-bold tracking-tight">{t('customers_title')}</h2>
              <p className="text-muted-foreground">{t('customers_page_description')}</p>
            </div>
            <Button onClick={() => navigate('/admin/customers/create')} className="w-fit">
              <Plus className="mr-2 h-4 w-4" />
              {t('customers_add_button')}
            </Button>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <MetricStatCard
              title={t('stat_total_customers_title')}
              value={page.stats.total}
              subtitle={t('stat_total_customers_subtitle')}
              icon={Contact}
              sparklineColor="#6366f1"
            />
            <MetricStatCard
              title={t('stat_with_address_title')}
              value={page.stats.withAddress}
              subtitle={t('stat_with_address_subtitle')}
              icon={MapPin}
              sparklineColor="#10b981"
            />
            <MetricStatCard
              title={t('stat_without_address_title')}
              value={page.stats.withoutAddress}
              subtitle={t('stat_without_address_subtitle')}
              icon={MapPinOff}
              sparklineColor="#94a3b8"
            />
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
            countLabel={t('customers_selected_count', { count: bulk.selectedCount })}
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
                <TabsTrigger value="all">{t('products_tab_all')}</TabsTrigger>
                <TabsTrigger value="true">{t('customers_tab_with_address')}</TabsTrigger>
                <TabsTrigger value="false">{t('customers_tab_without_address')}</TabsTrigger>
              </TabsList>
              <div className="flex items-center gap-2 lg:ml-auto">
                <div className="relative w-full sm:w-72">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder={t('customers_search_placeholder')}
                    value={searchTerm}
                    onChange={(e) => handleSearch(e.target.value)}
                    className="pl-8 pr-8"
                  />
                  {searchTerm && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="absolute right-1 top-1/2 h-7 w-7 -translate-y-1/2 text-muted-foreground"
                      onClick={() => handleSearch('')}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  )}
                </div>
                <DataTableViewOptions table={table} columnLabels={customerColumnLabels(t)} />
              </div>
            </div>

            <Card>
              <CardHeader>
                <CardTitle>{t('customers_title')}</CardTitle>
                <CardDescription>{t('page_of', { current: page.page, total: page.totalPages })}</CardDescription>
              </CardHeader>
              <CardContent className="px-0">
                <DataTable
                  table={table}
                  colCount={columns.length}
                  emptyMessage={t('customers_empty')}
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
