import { createColumnHelper } from '@tanstack/react-table'
import { MoreHorizontal, Eye } from 'lucide-react'
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
import { formatMoney } from '@/services/products-api'
import type { OrderSummary } from '@/services/orders-api'
import { OrderStatusBadge } from './components/order-status-badge'

const helper = createColumnHelper<OrderSummary>()

interface Options {
  t: (key: string, params?: Record<string, string | number>) => string
  onView: (id: string) => void
  selectedIds: Set<string>
  allPageSelected: boolean
  toggleSelectAll: (checked: boolean) => void
  toggleSelectRow: (id: string, checked: boolean) => void
}

export function buildOrdersColumns({
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
    helper.accessor('id', {
      id: 'order',
      header: () => t('orders_table_order'),
      cell: (info) => <span className="font-mono text-sm">{info.getValue().slice(0, 8)}</span>,
    }),
    helper.accessor('status', {
      id: 'status',
      header: () => t('orders_table_status'),
      cell: (info) => <OrderStatusBadge status={info.getValue()} />,
    }),
    helper.display({
      id: 'total',
      header: () => t('orders_table_total'),
      cell: (info) => {
        const o = info.row.original
        return o.totalAmountMinorUnits != null && o.currency ? formatMoney(o.totalAmountMinorUnits, o.currency) : '—'
      },
    }),
    helper.accessor('createdAt', {
      id: 'created',
      header: () => t('orders_table_created'),
      cell: (info) => (
        <span className="text-sm text-muted-foreground">{new Date(info.getValue()).toLocaleDateString()}</span>
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

export const orderColumnLabels = (t: (key: string) => string): Record<string, string> => ({
  order: t('orders_table_order'),
  status: t('orders_table_status'),
  total: t('orders_table_total'),
  created: t('orders_table_created'),
})
