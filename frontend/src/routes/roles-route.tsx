import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { RoutePending } from '@/components/route-pending'
import NotFoundError from '@/pages/errors/not-found-error'
import { rolesApi } from '@/services/roles-api'
import RolesIndex from '@/pages/roles'
import RoleShow from '@/pages/roles/show'

export default function RolesRoute() {
  const { id } = useParams()

  const listQuery = useQuery({
    queryKey: ['roles'],
    queryFn: () => rolesApi.list(),
    enabled: !id,
  })

  const detailQuery = useQuery({
    queryKey: ['roles', id],
    queryFn: () => rolesApi.detail(id!),
    enabled: !!id,
  })

  if (id) {
    if (detailQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return <RoleShow role={detailQuery.data} />
  }

  if (listQuery.isLoading) return <RoutePending />
  return <RolesIndex roles={listQuery.data ?? []} />
}
