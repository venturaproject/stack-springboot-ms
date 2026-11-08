import { AlertTriangle, ShoppingCart } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useI18n } from '@/i18n/context'
import { formatMoney, type Product } from '@/services/products-api'
import type { OrderSummary } from '@/services/orders-api'
import { OrderStatusBadge } from '@/pages/orders/components/order-status-badge'

interface Props {
  restockProducts: Product[]
  recentOrders: OrderSummary[]
}

export function ReportsTab({ restockProducts, recentOrders }: Props) {
  const { t } = useI18n()
  const navigate = useNavigate()

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5" />
            {t('reports_restock_title')}
          </CardTitle>
          <CardDescription>{t('reports_restock_description')}</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('products_table_sku')}</TableHead>
                <TableHead>{t('products_table_name')}</TableHead>
                <TableHead>{t('products_table_stock')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {restockProducts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="h-20 text-center text-muted-foreground">
                    {t('reports_restock_empty')}
                  </TableCell>
                </TableRow>
              ) : (
                restockProducts.map((product) => (
                  <TableRow
                    key={product.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/admin/products/${product.id}`)}
                  >
                    <TableCell className="font-mono text-xs">{product.sku}</TableCell>
                    <TableCell className="font-medium">{product.name}</TableCell>
                    <TableCell>
                      <Badge variant="destructive">{product.stockQuantity}</Badge>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ShoppingCart className="h-5 w-5" />
            {t('reports_recent_orders_title')}
          </CardTitle>
          <CardDescription>{t('reports_recent_orders_description')}</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('orders_table_order')}</TableHead>
                <TableHead>{t('orders_table_status')}</TableHead>
                <TableHead>{t('orders_table_total')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentOrders.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="h-20 text-center text-muted-foreground">
                    {t('orders_empty')}
                  </TableCell>
                </TableRow>
              ) : (
                recentOrders.map((order) => (
                  <TableRow
                    key={order.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/admin/orders/${order.id}`)}
                  >
                    <TableCell className="font-mono text-sm">{order.id.slice(0, 8)}</TableCell>
                    <TableCell>
                      <OrderStatusBadge status={order.status} />
                    </TableCell>
                    <TableCell>
                      {order.totalAmountMinorUnits != null && order.currency
                        ? formatMoney(order.totalAmountMinorUnits, order.currency)
                        : '—'}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
