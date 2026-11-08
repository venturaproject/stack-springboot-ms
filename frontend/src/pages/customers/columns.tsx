import { createColumnHelper } from '@tanstack/react-table'
import { MoreHorizontal, Eye, ShoppingCart } from 'lucide-react'
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
import type { Customer } from '@/services/customers-api'

const helper = createColumnHelper<Customer>()

interface Options {
  t: (key: string, params?: Record<string, string | number>) => string
  onView: (id: string) => void
  onNewOrder: (id: string) => void
  selectedIds: Set<string>
  allPageSelected: boolean
  toggleSelectAll: (checked: boolean) => void
  toggleSelectRow: (id: string, checked: boolean) => void
}

export function buildCustomersColumns({
  t,
  onView,
  onNewOrder,
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
      header: () => t('customers_table_name'),
      cell: (info) => <span className="font-medium">{info.getValue()}</span>,
    }),
    helper.accessor('email', {
      id: 'email',
      header: () => t('customers_table_email'),
      cell: (info) => <span className="text-sm text-muted-foreground">{info.getValue()}</span>,
    }),
    helper.display({
      id: 'address',
      header: () => t('customers_table_address'),
      cell: (info) => {
        const a = info.row.original.address
        return a ? (
          <span className="text-sm text-muted-foreground">
            {a.city}, {a.country}
          </span>
        ) : (
          <Badge variant="outline">{t('common_none')}</Badge>
        )
      },
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
            <DropdownMenuItem onClick={() => onNewOrder(info.row.original.id)}>
              <ShoppingCart className="mr-2 h-4 w-4" />
              {t('customers_new_order_tooltip')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    }),
  ]
}

export const customerColumnLabels = (t: (key: string) => string): Record<string, string> => ({
  name: t('customers_table_name'),
  email: t('customers_table_email'),
  address: t('customers_table_address'),
})
