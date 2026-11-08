import { useQuery } from '@tanstack/react-query'
import { useParams, useLocation, useSearchParams } from 'react-router-dom'
import { RoutePending } from '@/components/route-pending'
import NotFoundError from '@/pages/errors/not-found-error'
import { customersApi } from '@/services/customers-api'
import CustomersIndex from '@/pages/customers'
import CustomerCreate from '@/pages/customers/create'
import CustomerShow from '@/pages/customers/show'
import CustomerEdit from '@/pages/customers/edit'

export default function CustomersRoute() {
  const { id } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const isCreate = location.pathname.endsWith('/customers/create')
  const isEdit = !!id && location.pathname.endsWith('/edit')
  const isShow = !!id && !isEdit && !isCreate

  const page = Number(searchParams.get('page') ?? 1)
  const size = Number(searchParams.get('size') ?? 20)
  const search = searchParams.get('search') ?? undefined
  const hasAddressParam = searchParams.get('hasAddress') ?? undefined
  const hasAddress = hasAddressParam === undefined ? undefined : hasAddressParam === 'true'

  const listQuery = useQuery({
    queryKey: ['customers', { page, size, search, hasAddress }],
    queryFn: () => customersApi.list({ page, size, search, hasAddress }),
    enabled: !isShow && !isEdit && !isCreate,
  })

  const detailQuery = useQuery({
    queryKey: ['customers', id],
    queryFn: () => customersApi.detail(id!),
    enabled: isShow || isEdit,
  })

  if (isCreate) return <CustomerCreate />

  if (isShow || isEdit) {
    if (detailQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return isEdit ? <CustomerEdit customer={detailQuery.data} /> : <CustomerShow customer={detailQuery.data} />
  }

  if (listQuery.isLoading) return <RoutePending />
  return <CustomersIndex page={listQuery.data} filters={{ search, hasAddress: hasAddressParam }} />
}
