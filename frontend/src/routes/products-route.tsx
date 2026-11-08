import { useQuery } from '@tanstack/react-query'
import { useParams, useLocation, useSearchParams } from 'react-router-dom'
import { RoutePending } from '@/components/route-pending'
import NotFoundError from '@/pages/errors/not-found-error'
import { productsApi } from '@/services/products-api'
import ProductsIndex from '@/pages/products'
import ProductCreate from '@/pages/products/create'
import ProductShow from '@/pages/products/show'
import ProductEdit from '@/pages/products/edit'

export default function ProductsRoute() {
  const { id } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const isCreate = location.pathname.endsWith('/products/create')
  const isEdit = !!id && location.pathname.endsWith('/edit')
  const isShow = !!id && !isEdit && !isCreate

  const page = Number(searchParams.get('page') ?? 1)
  const size = Number(searchParams.get('size') ?? 20)
  const search = searchParams.get('search') ?? undefined

  const listQuery = useQuery({
    queryKey: ['products', { page, size, search }],
    queryFn: () => productsApi.list({ page, size, search }),
    enabled: !isShow && !isEdit && !isCreate,
  })

  const detailQuery = useQuery({
    queryKey: ['products', id],
    queryFn: () => productsApi.detail(id!),
    enabled: isShow || isEdit,
  })

  if (isCreate) return <ProductCreate />

  if (isShow || isEdit) {
    if (detailQuery.isLoading) return <RoutePending />
    if (detailQuery.isError || !detailQuery.data) return <NotFoundError />
    return isEdit ? <ProductEdit product={detailQuery.data} /> : <ProductShow product={detailQuery.data} />
  }

  if (listQuery.isLoading) return <RoutePending />
  return <ProductsIndex page={listQuery.data} filters={{ search }} />
}
