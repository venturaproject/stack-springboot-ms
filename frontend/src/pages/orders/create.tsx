import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ShoppingCart } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { RoutePending } from '@/components/route-pending'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import { getErrorMessage } from '@/lib/errors'
import { useI18n } from '@/i18n/context'
import { formatMoney } from '@/lib/format'
import { customersApi } from '@/services/customers-api'
import { productsApi } from '@/services/products-api'
import { ordersApi } from '@/services/orders-api'

export default function OrderCreate() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [customerId, setCustomerId] = useState('')
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [busy, setBusy] = useState(false)

  const customers = useQuery({ queryKey: ['customers', { size: 200 }], queryFn: () => customersApi.list({ size: 200 }) })
  const products = useQuery({ queryKey: ['products', { size: 200 }], queryFn: () => productsApi.list({ size: 200 }) })

  if (customers.isLoading || products.isLoading) return <RoutePending />

  const start = async () => {
    if (!customerId || !productId || Number(quantity) < 1) {
      toast.error(t('order_create_missing_fields'))
      return
    }
    setBusy(true)
    try {
      const order = await ordersApi.addLine({ customerId, productId, quantity: Number(quantity) })
      qc.invalidateQueries({ queryKey: ['orders'] })
      toast.success(t('order_create_success'))
      navigate(`/admin/orders/${order.id}`)
    } catch (err) {
      toast.error(getErrorMessage(err, t('order_create_error')))
    } finally {
      setBusy(false)
    }
  }

  const selectClass = 'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm'

  return (
    <AuthenticatedLayout title={t('order_create_title')}>
      <Main>
        <div className="mx-auto grid w-full max-w-2xl flex-1 items-start gap-4 md:gap-6">
          <FormHeader
            backTo="/admin/orders"
            title={t('order_create_title')}
            cancelTo="/admin/orders"
            submitting={busy}
            submitLabel={t('order_create_submit')}
            onSubmit={start}
          />

          <Card>
            <CardHeader className="items-center text-center">
              <div className="mb-1 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
                <ShoppingCart className="h-6 w-6" />
              </div>
              <CardTitle>{t('order_create_card_title')}</CardTitle>
              <CardDescription>{t('order_create_card_description')}</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="grid gap-2">
                <Label htmlFor="customer">{t('order_show_customer_label')}</Label>
                <select
                  id="customer"
                  className={selectClass}
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                >
                  <option value="">{t('order_field_customer_placeholder')}</option>
                  {(customers.data?.data ?? []).map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} · {c.email}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="product">{t('order_show_table_product')}</Label>
                <select
                  id="product"
                  className={selectClass}
                  value={productId}
                  onChange={(e) => setProductId(e.target.value)}
                >
                  <option value="">{t('order_field_product_placeholder')}</option>
                  {(products.data?.data ?? []).map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} — {formatMoney(p.priceAmountMinorUnits, p.priceCurrency)} ({p.stockQuantity})
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="qty">{t('order_show_table_qty')}</Label>
                <Input id="qty" type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
              </div>
            </CardContent>
          </Card>

          <FormMobileActions
            cancelTo="/admin/orders"
            submitting={busy}
            submitLabel={t('order_create_submit')}
            onSubmit={start}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
