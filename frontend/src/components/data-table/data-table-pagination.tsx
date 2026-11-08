import {
  ChevronsLeft,
  ChevronsRight,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { CardFooter } from '@/components/ui/card'
import { useI18n } from '@/i18n/context'
import { PAGINATION_OPTIONS } from '@/lib/constants'

interface DataTablePaginationProps {
  /** 1-based current page (backend `PagedResponse.page`). */
  page: number
  /** Total pages (backend `PagedResponse.totalPages`). */
  totalPages: number
  /** Rows per page (backend `PagedResponse.perPage`). */
  perPage: number
  /** Total rows (backend `PagedResponse.count`). */
  count: number
  selectedCount: number
  onPageChange: (page: number) => void
  onPerPageChange: (value: string) => void
}

export function DataTablePagination({
  page,
  totalPages,
  perPage,
  count,
  selectedCount,
  onPageChange,
  onPerPageChange,
}: DataTablePaginationProps) {
  const { t } = useI18n()

  return (
    <CardFooter className="flex items-center justify-between gap-4 py-4">
      <span className="text-sm text-muted-foreground">
        {t('n_rows_selected', { selected: selectedCount, total: count })}
      </span>
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground whitespace-nowrap">
            {t('rows_per_page')}
          </span>
          <Select value={String(perPage)} onValueChange={onPerPageChange}>
            <SelectTrigger className="h-8 w-[70px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {PAGINATION_OPTIONS.map((n) => (
                <SelectItem key={n} value={String(n)}>
                  {n}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <span className="text-sm text-muted-foreground whitespace-nowrap">
          {t('page_of', { current: page, total: totalPages })}
        </span>
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => onPageChange(1)}
            disabled={page === 1}
          >
            <ChevronsLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => onPageChange(page - 1)}
            disabled={page === 1}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => onPageChange(totalPages)}
            disabled={page >= totalPages}
          >
            <ChevronsRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </CardFooter>
  )
}
