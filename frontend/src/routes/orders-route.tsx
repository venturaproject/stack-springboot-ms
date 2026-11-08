import { useQuery } from '@tanstack/react-query'
import { useParams, useLocation, useSearchParams } from 'react-router-dom'
import { RoutePending } from '@/components/route-pending'
import NotFoundError from '@/pages/errors/not-found-error'
import { ordersApi, type OrderStatus } from '@/services/orders-api'
import OrdersIndex from '@/pages/orders'
import OrderCreate from '@/pages/orders/create'
import OrderShow from '@/pages/orders/show'

export default function OrdersRoute() {
  const { id } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const isCreate = location.pathname.endsWith('/orders/create')
  const isShow = !!id && !isCreate

  const page = Number(searchParams.get('page') ?? 1)
  const size = Number(searchParams.get('size') ?? 20)
  const status = (searchParams.get('status') as OrderStatus | null) ?? undefined

  const listQuery = useQuery({
    queryKey: ['orders', { page, size, status }],
    queryFn: () => ordersApi.list({ page, size, status }),
    enabled: !isShow && !isCreate,
  })

  const detailQuery = useQuery({
    queryKey: ['orders', id],
    queryFn: () => ordersApi.detail(id!),
    enabled: isShow,
  })

  if (isCreate) return <OrderCreate />

  if (isShow) {
    if (detailQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return <OrderShow order={detailQuery.data} />
  }

  if (listQuery.isLoading) return <RoutePending />
  return <OrdersIndex page={listQuery.data} filters={{ status }} />
}
