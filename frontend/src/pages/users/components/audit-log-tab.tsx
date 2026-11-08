import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DataTablePagination } from '@/components/data-table'
import { useI18n } from '@/i18n/context'
import { usersApi } from '@/services/users-api'

/** Shared by the user show and edit pages — one audit trail table for both. */
export function AuditLogTab({ userId }: { userId: string }) {
  const { t } = useI18n()
  const [page, setPage] = useState(1)
  const size = 10

  const { data, isLoading } = useQuery({
    queryKey: ['audit-log', userId, page],
    queryFn: () => usersApi.auditLog(userId, { page, size }),
  })

  if (isLoading) {
    return <p className="py-8 text-center text-sm text-muted-foreground">{t('common_loading')}</p>
  }

  if (!data?.data.length) {
    return <p className="py-8 text-center text-sm text-muted-foreground">{t('users_audit_log_empty')}</p>
  }

  return (
    <div className="space-y-4">
      <Card className="overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('common_date')}</TableHead>
              <TableHead>{t('users_audit_action_header')}</TableHead>
              <TableHead>{t('users_audit_resource_header')}</TableHead>
              <TableHead>{t('users_audit_changes_header')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.data.map((row) => (
              <TableRow key={row.id}>
                <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                  {new Date(row.createdAt).toLocaleString()}
                </TableCell>
                <TableCell>
                  <Badge variant="secondary">{row.action}</Badge>
                </TableCell>
                <TableCell className="text-sm">
                  {row.entity}
                  {row.entityId && (
                    <span className="ml-1 font-mono text-xs text-muted-foreground">
                      #{row.entityId.slice(0, 8)}
                    </span>
                  )}
                </TableCell>
                <TableCell className="max-w-xs truncate font-mono text-xs text-muted-foreground" title={row.changes ?? ''}>
                  {row.changes ?? '—'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <DataTablePagination
          page={data.page}
          totalPages={data.totalPages}
          perPage={data.perPage}
          count={data.count}
          selectedCount={0}
          onPageChange={setPage}
          onPerPageChange={() => {}}
        />
      </Card>
    </div>
  )
}
