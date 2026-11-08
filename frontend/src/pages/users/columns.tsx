import { createColumnHelper } from '@tanstack/react-table'
import { MoreHorizontal, Eye, Pencil, Trash2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import type { UserDto } from '@/services/users-api'
import { UserStatusBadge } from './components/user-status-badge'

const helper = createColumnHelper<UserDto>()

interface Options {
  t: (key: string, params?: Record<string, string | number>) => string
  selectedIds: Set<string>
  allPageSelected: boolean
  toggleSelectAll: (checked: boolean) => void
  toggleSelectRow: (id: string, checked: boolean) => void
  onView: (id: string) => void
  onEdit: (id: string) => void
  onDelete: (u: UserDto) => void
}

export function buildUsersColumns({
  t,
  selectedIds,
  allPageSelected,
  toggleSelectAll,
  toggleSelectRow,
  onView,
  onEdit,
  onDelete,
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
    helper.display({
      id: 'name',
      header: () => t('user_field_name'),
      cell: (info) => {
        const u = info.row.original
        return (
          <div className="flex items-center gap-3">
            <Avatar className="h-8 w-8 shrink-0">
              {u.avatarUrl && <AvatarImage src={u.avatarUrl} alt={u.firstName} />}
              <AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">
                {((u.firstName[0] ?? '') + (u.lastName[0] ?? '')).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">
                {u.firstName} {u.lastName}
              </p>
              <p className="truncate text-xs text-muted-foreground">{u.email}</p>
            </div>
          </div>
        )
      },
    }),
    helper.accessor('roles', {
      id: 'roles',
      header: () => t('roles_title'),
      cell: (info) => (
        <div className="flex flex-wrap gap-1">
          {info.getValue().map((r) => (
            <Badge key={r} variant="outline">
              {r}
            </Badge>
          ))}
        </div>
      ),
    }),
    helper.accessor('active', {
      id: 'status',
      header: () => t('common_status'),
      cell: (info) => <UserStatusBadge active={info.getValue()} />,
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
              {t('common_view')}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onEdit(info.row.original.id)}>
              <Pencil className="mr-2 h-4 w-4" />
              {t('common_edit')}
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => onDelete(info.row.original)}
              className="text-destructive focus:text-destructive"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              {t('delete')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    }),
  ]
}

export const userColumnLabels = (t: (key: string) => string): Record<string, string> => ({
  name: t('user_field_name'),
  roles: t('roles_title'),
  status: t('common_status'),
})
