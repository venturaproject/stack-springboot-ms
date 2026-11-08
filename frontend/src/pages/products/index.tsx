import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import { useNavigate } from 'react-router-dom'
import { Package, Plus, Search, X, Download } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { MetricStatCard } from '@/components/metric-stat-card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
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
import { formatMoney, type Product } from '@/services/products-api'
import { buildProductsColumns, productColumnLabels } from './columns'

const LOW_STOCK = 5

interface ProductFilters {
  search?: string
  [key: string]: string | undefined
}

interface Props {
  page?: Paginated<Product>
  filters?: ProductFilters
}

export default function ProductsIndex({ page = emptyPage<Product>(), filters: initial = {} }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const bulk = useBulkSelection(page.data.map((p) => p.id), page.count)

  const { searchTerm, handleSearch, handlePageChange, handleSizeChange } = useTableFilters<ProductFilters>({
    basePath: '/admin/products',
    initialFilters: initial,
    initialSize: page.perPage,
    onNavigate: bulk.clearSelection,
  })

  const columns = buildProductsColumns({
    t,
    onView: (id) => navigate(`/admin/products/${id}`),
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

  const lowStock = page.data.filter((p) => p.stockQuantity <= LOW_STOCK).length
  const outOfStock = page.data.filter((p) => p.stockQuantity === 0).length

  const exportSelected = () => {
    const rows = page.data.filter((p) => bulk.selectedIds.has(p.id))
    downloadCsv('products.csv', rows, [
      { header: 'SKU', value: (p) => p.sku },
      { header: 'Name', value: (p) => p.name },
      { header: 'Price', value: (p) => formatMoney(p.priceAmountMinorUnits, p.priceCurrency) },
      { header: 'Stock', value: (p) => p.stockQuantity },
    ])
  }

  return (
    <AuthenticatedLayout title={t('products_title')}>
      <Main>
        <div className="grid flex-1 items-start gap-4 md:gap-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-2xl font-bold tracking-tight">{t('products_title')}</h2>
              <p className="text-muted-foreground">{t('products_page_description')}</p>
            </div>
            <Button onClick={() => navigate('/admin/products/create')} className="w-fit">
              <Plus className="mr-2 h-4 w-4" />
              {t('products_add_button')}
            </Button>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <MetricStatCard
              title={t('stat_total_products_title')}
              value={page.count}
              subtitle={t('stat_total_products_subtitle')}
              icon={Package}
              sparklineColor="#6366f1"
            />
            <MetricStatCard
              title={t('products_tab_low_stock')}
              value={lowStock}
              subtitle={t('stat_low_stock_subtitle')}
              icon={Package}
              sparklineColor="#f59e0b"
            />
            <MetricStatCard
              title={t('products_tab_out_of_stock')}
              value={outOfStock}
              subtitle={t('products_tab_out_of_stock')}
              icon={Package}
              sparklineColor="#ef4444"
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
            countLabel={t('products_selected_count', { count: bulk.selectedCount })}
            actions={
              <DropdownMenuItem onClick={exportSelected}>
                <Download className="mr-2 h-4 w-4" />
                {t('common_export_csv')}
              </DropdownMenuItem>
            }
          />

          <Card>
            <CardHeader className="gap-3">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <CardTitle>{t('products_title')}</CardTitle>
                  <CardDescription>{t('page_of', { current: page.page, total: page.totalPages })}</CardDescription>
                </div>
                <div className="flex items-center gap-2">
                  <div className="relative w-full sm:w-72">
                    <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      placeholder={t('products_search_placeholder')}
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
                  <DataTableViewOptions table={table} columnLabels={productColumnLabels(t)} />
                </div>
              </div>
            </CardHeader>
            <CardContent className="px-0">
              <DataTable
                table={table}
                colCount={columns.length}
                emptyMessage={t('products_empty')}
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
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
