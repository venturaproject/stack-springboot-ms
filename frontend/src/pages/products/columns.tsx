import { createColumnHelper } from '@tanstack/react-table'
import { MoreHorizontal, Eye } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { formatMoney, type Product } from '@/services/products-api'

const LOW_STOCK = 5
const helper = createColumnHelper<Product>()

interface Options {
  t: (key: string, params?: Record<string, string | number>) => string
  onView: (id: string) => void
  selectedIds: Set<string>
  allPageSelected: boolean
  toggleSelectAll: (checked: boolean) => void
  toggleSelectRow: (id: string, checked: boolean) => void
}

export function buildProductsColumns({
  t,
  onView,
  selectedIds,
  allPageSelected,
  toggleSelectAll,
  toggleSelectRow,
}: Options) {
  return [
    helper.display({
      id: 'select',
      enableHiding: false,
      header: () => (
        <Checkbox checked={allPageSelected} onCheckedChange={(c) => toggleSelectAll(!!c)} aria-label="Select all" />
      ),
      cell: (info) => (
        <Checkbox
          checked={selectedIds.has(info.row.original.id)}
          onCheckedChange={(c) => toggleSelectRow(info.row.original.id, !!c)}
          onClick={(e) => e.stopPropagation()}
          aria-label="Select row"
        />
      ),
    }),
    helper.accessor('name', {
      id: 'name',
      header: () => t('products_table_name'),
      cell: (info) => (
        <div>
          <div className="font-medium">{info.getValue()}</div>
          {info.row.original.description && (
            <div className="line-clamp-1 text-sm text-muted-foreground">{info.row.original.description}</div>
          )}
        </div>
      ),
    }),
    helper.accessor('sku', {
      id: 'sku',
      header: () => t('products_table_sku'),
      cell: (info) => <span className="font-mono text-sm text-muted-foreground">{info.getValue()}</span>,
    }),
    helper.display({
      id: 'price',
      header: () => t('products_table_price'),
      cell: (info) => formatMoney(info.row.original.priceAmountMinorUnits, info.row.original.priceCurrency),
    }),
    helper.accessor('stockQuantity', {
      id: 'stock',
      header: () => t('products_table_stock'),
      cell: (info) => (
        <Badge variant={info.getValue() <= LOW_STOCK ? 'destructive' : 'secondary'}>{info.getValue()}</Badge>
      ),
    }),
    helper.display({
      id: 'actions',
      enableHiding: false,
      header: () => <span className="sr-only">{t('common_actions')}</span>,
      cell: (info) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button aria-haspopup="true" size="icon" variant="ghost" onClick={(e) => e.stopPropagation()}>
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>{t('common_actions')}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => onView(info.row.original.id)}>
              <Eye className="mr-2 h-4 w-4" />
              {t('common_open')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    }),
  ]
}

export const productColumnLabels = (t: (key: string) => string): Record<string, string> => ({
  name: t('products_table_name'),
  sku: t('products_table_sku'),
  price: t('products_table_price'),
  stock: t('products_table_stock'),
})
