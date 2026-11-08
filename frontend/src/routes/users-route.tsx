import { useQuery } from '@tanstack/react-query'
import { useParams, useLocation, useSearchParams } from 'react-router-dom'
import { RoutePending } from '@/components/route-pending'
import NotFoundError from '@/pages/errors/not-found-error'
import { usersApi } from '@/services/users-api'
import { rolesApi } from '@/services/roles-api'
import UsersIndex from '@/pages/users'
import UserForm from '@/pages/users/form'
import UserShow from '@/pages/users/show'

export default function UsersRoute() {
  const { id } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const isCreate = location.pathname.endsWith('/users/create')
  const isEdit = !!id && location.pathname.endsWith('/edit')
  const isShow = !!id && !isEdit

  const page = Number(searchParams.get('page') ?? 1)
  const size = Number(searchParams.get('size') ?? 20)
  const search = searchParams.get('search') ?? undefined

  const listQuery = useQuery({
    queryKey: ['users', { page, size, search }],
    queryFn: () => usersApi.list({ page, size, search }),
    enabled: !isCreate && !isEdit && !isShow,
  })

  const rolesQuery = useQuery({
    queryKey: ['roles'],
    queryFn: () => rolesApi.list(),
    enabled: isCreate || isEdit,
  })

  const detailQuery = useQuery({
    queryKey: ['users', id],
    queryFn: () => usersApi.detail(id!),
    enabled: !!id,
  })

  if (isCreate) return <UserForm roles={rolesQuery.data ?? []} />

  if (isEdit) {
    if (detailQuery.isLoading || rolesQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return <UserForm user={detailQuery.data} roles={rolesQuery.data ?? []} />
  }

  if (isShow) {
    if (detailQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return <UserShow user={detailQuery.data} />
  }

  if (listQuery.isLoading) return <RoutePending />
  return <UsersIndex page={listQuery.data} filters={{ search }} />
}
