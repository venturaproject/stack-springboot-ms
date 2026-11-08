import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ChevronLeft, ShoppingCart, CheckCircle2, XCircle, Loader2, Trash2, Plus } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useI18n } from '@/i18n/context'
import { formatMoney } from '@/lib/format'
import { productsApi } from '@/services/products-api'
import { ordersApi, type Order } from '@/services/orders-api'
import { getErrorMessage } from '@/lib/errors'
import { OrderStatusBadge } from './components/order-status-badge'

export default function OrderShow({ order }: { order: Order }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [busy, setBusy] = useState(false)
  const [lineBusy, setLineBusy] = useState<string | null>(null)
  const [addProductId, setAddProductId] = useState('')
  const [addQty, setAddQty] = useState('1')

  const isDraft = order.status === 'DRAFT'
  const products = useQuery({
    queryKey: ['products', { size: 200 }],
    queryFn: () => productsApi.list({ size: 200 }),
    enabled: isDraft,
  })

  const refresh = () => qc.invalidateQueries({ queryKey: ['orders'] })

  const run = async (fn: () => Promise<unknown>, ok?: string) => {
    setBusy(true)
    try {
      await fn()
      if (ok) toast.success(ok)
      refresh()
    } catch (err) {
      toast.error(getErrorMessage(err, t('something_went_wrong')))
    } finally {
      setBusy(false)
    }
  }

  const address = order.shippingAddress
    ? [
        order.shippingAddress.street,
        order.shippingAddress.city,
        order.shippingAddress.postalCode,
        order.shippingAddress.country,
      ]
        .filter(Boolean)
        .join(', ')
    : '—'

  const available = (products.data?.data ?? []).filter((p) => !order.lines.some((l) => l.productId === p.id))
  const selectClass = 'flex h-9 flex-1 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm'
  const total =
    order.totalAmountMinorUnits != null && order.currency
      ? formatMoney(order.totalAmountMinorUnits, order.currency)
      : '—'

  return (
    <AuthenticatedLayout title={`Order ${order.id.slice(0, 8)}`}>
      <Main>
        <div className="grid max-w-3xl flex-1 items-start gap-4 md:gap-8">
          <div className="flex items-center gap-4">
            <Button variant="outline" size="icon" onClick={() => navigate('/admin/orders')}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <h1 className="flex items-center gap-2 text-xl font-semibold">
              <ShoppingCart className="h-5 w-5" />
              Order <span className="font-mono">{order.id.slice(0, 8)}</span>
            </h1>
            <OrderStatusBadge status={order.status} />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>{t('order_show_customer_label')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-muted-foreground">{t('order_show_customer_label')}</p>
                  <p className="font-mono font-medium">{order.customerId.slice(0, 8)}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('order_show_address_label')}</p>
                  <p className="font-medium">{address}</p>
                </div>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('order_show_table_sku')}</TableHead>
                    <TableHead>{t('order_show_table_product')}</TableHead>
                    <TableHead className="text-right">{t('order_show_table_unit_price')}</TableHead>
                    <TableHead className="w-24 text-right">{t('order_show_table_qty')}</TableHead>
                    <TableHead className="text-right">{t('order_show_table_line_total')}</TableHead>
                    {isDraft && <TableHead className="w-10" />}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {order.lines.map((line) => (
                    <TableRow key={line.id}>
                      <TableCell className="font-mono text-xs">{line.productSku}</TableCell>
                      <TableCell>{line.productName}</TableCell>
                      <TableCell className="text-right">
                        {formatMoney(line.unitPriceAmountMinorUnits, line.currency)}
                      </TableCell>
                      <TableCell className="text-right">
                        {isDraft ? (
                          <Input
                            type="number"
                            min="1"
                            className="ml-auto h-8 w-20 text-right"
                            defaultValue={line.quantity}
                            disabled={lineBusy === line.id}
                            onBlur={(e) => {
                              const q = Number(e.target.value)
                              if (q >= 1 && q !== line.quantity) {
                                setLineBusy(line.id)
                                run(() => ordersApi.updateLineQuantity(order.id, line.id, q)).finally(() =>
                                  setLineBusy(null),
                                )
                              }
                            }}
                          />
                        ) : (
                          line.quantity
                        )}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatMoney(line.lineTotalMinorUnits, line.currency)}
                      </TableCell>
                      {isDraft && (
                        <TableCell>
                          <Button
                            size="icon"
                            variant="ghost"
                            disabled={busy}
                            onClick={() => run(() => ordersApi.removeLine(order.id, line.id), t('order_line_removed'))}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                  {order.lines.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={isDraft ? 6 : 5} className="h-16 text-center text-muted-foreground">
                        —
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>

              {isDraft && (
                <div className="flex items-center gap-2 border-t pt-4">
                  <select className={selectClass} value={addProductId} onChange={(e) => setAddProductId(e.target.value)}>
                    <option value="">{t('order_field_product_placeholder')}</option>
                    {available.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} — {formatMoney(p.priceAmountMinorUnits, p.priceCurrency)}
                      </option>
                    ))}
                  </select>
                  <Input
                    type="number"
                    min="1"
                    className="w-20"
                    value={addQty}
                    onChange={(e) => setAddQty(e.target.value)}
                  />
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={busy || !addProductId || Number(addQty) < 1}
                    onClick={() =>
                      run(async () => {
                        await ordersApi.addLine({ orderId: order.id, productId: addProductId, quantity: Number(addQty) })
                        setAddProductId('')
                        setAddQty('1')
                      }, t('order_line_added'))
                    }
                  >
                    <Plus className="mr-1 h-4 w-4" />
                    {t('order_add_product_button')}
                  </Button>
                </div>
              )}

              <div className="flex justify-end border-t pt-4 text-lg font-semibold">
                {t('order_show_total_label')}: {total}
              </div>

              {(order.status === 'DRAFT' || order.status === 'PLACED') && (
                <div className="flex justify-end gap-2 border-t pt-4">
                  <Button variant="outline" disabled={busy} onClick={() => run(() => ordersApi.cancel(order.id), t('order_cancel_success'))}>
                    {busy ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <XCircle className="mr-2 h-4 w-4" />}
                    {t('order_cancel_button')}
                  </Button>
                  {isDraft && (
                    <Button
                      disabled={busy || order.lines.length === 0}
                      onClick={() => run(() => ordersApi.place(order.id), t('order_place_success'))}
                    >
                      {busy ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <CheckCircle2 className="mr-2 h-4 w-4" />}
                      {t('order_place_button')}
                    </Button>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
