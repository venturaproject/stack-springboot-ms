import { useQuery } from '@tanstack/react-query'
import { RoutePending } from '@/components/route-pending'
import { permissionsApi } from '@/services/permissions-api'
import PermissionsIndex from '@/pages/permissions'

export default function PermissionsRoute() {
  const { data, isLoading } = useQuery({
    queryKey: ['permissions'],
    queryFn: () => permissionsApi.list(),
  })

  if (isLoading) return <RoutePending />
  return <PermissionsIndex permissions={data ?? []} />
}
