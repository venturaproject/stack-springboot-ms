import { useNavigate } from 'react-router-dom'
import { Pencil, Package } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { DetailHeader } from '@/components/page/detail-header'
import { InfoRow } from '@/components/page/info-row'
import { useI18n } from '@/i18n/context'
import { formatDate, formatMoney } from '@/lib/format'
import type { Product } from '@/services/products-api'

export default function ProductShow({ product }: { product: Product }) {
  const { t } = useI18n()
  const navigate = useNavigate()

  const lowOrOut = product.stockQuantity <= 5

  return (
    <AuthenticatedLayout title={product.name}>
      <Main>
        <div className="mx-auto grid max-w-3xl flex-1 items-start gap-6 md:gap-8">
          <DetailHeader backTo="/admin/products" title={product.name} subtitle={product.sku} subtitleMono>
            <Button onClick={() => navigate(`/admin/products/${product.id}/edit`)}>
              <Pencil className="mr-2 h-4 w-4" />
              {t('common_edit')}
            </Button>
          </DetailHeader>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="h-5 w-5" />
                {t('products_show_details_title')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="mb-4 flex h-40 w-40 items-center justify-center overflow-hidden rounded-md border bg-muted">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
                ) : (
                  <Package className="h-10 w-10 text-muted-foreground" />
                )}
              </div>
              <dl className="space-y-1">
                <InfoRow label={t('products_table_name')} value={product.name} />
                <Separator />
                <InfoRow label={t('products_table_sku')} value={<span className="font-mono">{product.sku}</span>} />
                <Separator />
                <InfoRow label={t('description')} value={product.description} />
                <Separator />
                <InfoRow
                  label={t('products_table_price')}
                  value={formatMoney(product.priceAmountMinorUnits, product.priceCurrency)}
                />
                <Separator />
                <div className="flex items-center justify-between py-3">
                  <dt className="text-sm text-muted-foreground">{t('products_table_stock')}</dt>
                  <dd>
                    <Badge variant={lowOrOut ? 'destructive' : 'secondary'}>{product.stockQuantity}</Badge>
                  </dd>
                </div>
                <Separator />
                <InfoRow label={t('common_created_at')} value={product.createdAt ? formatDate(product.createdAt) : '—'} />
              </dl>
            </CardContent>
          </Card>
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
