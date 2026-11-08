import { useState } from 'react'
import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Users as UsersIcon, Plus, Search, X, Download } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { ConfirmDialog } from '@/components/confirm-dialog'
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
import { usersApi, type UserDto } from '@/services/users-api'
import { AccessControlTabs } from './access-control-tabs'
import { UserAreaTabs } from './user-area-tabs'
import { buildUsersColumns, userColumnLabels } from './columns'

interface UserFilters {
  search?: string
  [key: string]: string | undefined
}

interface Props {
  page?: Paginated<UserDto>
  filters?: UserFilters
}

export default function UsersIndex({ page = emptyPage<UserDto>(), filters: initial = {} }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [toDelete, setToDelete] = useState<UserDto | null>(null)
  const bulk = useBulkSelection(page.data.map((u) => u.id), page.count)

  const { searchTerm, handleSearch, handlePageChange, handleSizeChange } = useTableFilters<UserFilters>({
    basePath: '/admin/users',
    initialFilters: initial,
    initialSize: page.perPage,
    onNavigate: bulk.clearSelection,
  })

  const columns = buildUsersColumns({
    t,
    selectedIds: bulk.selectedIds,
    allPageSelected: bulk.allPageSelected,
    toggleSelectAll: bulk.toggleSelectAll,
    toggleSelectRow: bulk.toggleSelectRow,
    onView: (id) => navigate(`/admin/users/${id}`),
    onEdit: (id) => navigate(`/admin/users/${id}/edit`),
    onDelete: setToDelete,
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

  const confirmDelete = async () => {
    if (!toDelete) return
    await usersApi.remove(toDelete.id)
    toast.success(t('user_deleted_successfully'))
    setToDelete(null)
    qc.invalidateQueries({ queryKey: ['users'] })
  }

  const exportSelected = () => {
    const rows = page.data.filter((u) => bulk.selectedIds.has(u.id))
    downloadCsv('users.csv', rows, [
      { header: 'Name', value: (u) => `${u.firstName} ${u.lastName}`.trim() },
      { header: 'Email', value: (u) => u.email },
      { header: 'Roles', value: (u) => u.roles.join(' | ') },
      { header: 'Active', value: (u) => (u.active ? 'yes' : 'no') },
    ])
  }

  return (
    <AuthenticatedLayout title={t('access_control_title')}>
      <Main>
        <div className="grid flex-1 items-start gap-4">
          <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
            <UsersIcon className="h-6 w-6" /> {t('access_control_title')}
          </h1>

          <AccessControlTabs hideUserSections />
          <UserAreaTabs />

          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold tracking-tight">{t('users_title')}</h2>
              <p className="text-sm text-muted-foreground">{t('manage_users_and_roles')}</p>
            </div>
            <Button onClick={() => navigate('/admin/users/create')} className="gap-1">
              <Plus className="h-4 w-4" /> {t('add_user')}
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
            countLabel={`${bulk.selectedCount}`}
            actions={
              <DropdownMenuItem onClick={exportSelected}>
                <Download className="mr-2 h-4 w-4" />
                {t('common_export_csv')}
              </DropdownMenuItem>
            }
          />

          <Card>
            <CardHeader className="gap-3">
              <div className="flex items-center justify-between gap-2">
                <div>
                  <CardTitle>{t('users_title')}</CardTitle>
                  <CardDescription>{t('page_of', { current: page.page, total: page.totalPages })}</CardDescription>
                </div>
                <div className="flex items-center gap-2">
                  <div className="relative w-full sm:w-64">
                    <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      placeholder={t('search')}
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
                  <DataTableViewOptions table={table} columnLabels={userColumnLabels(t)} />
                </div>
              </div>
            </CardHeader>
            <CardContent className="px-0">
              <DataTable
                table={table}
                colCount={columns.length}
                emptyMessage={t('common_no_results')}
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

        <ConfirmDialog
          open={!!toDelete}
          onOpenChange={(o) => !o && setToDelete(null)}
          title={t('delete_user')}
          desc={`${toDelete?.email}`}
          destructive
          confirmText={t('delete')}
          handleConfirm={confirmDelete}
        />
      </Main>
    </AuthenticatedLayout>
  )
}
