import { useNavigate } from 'react-router-dom'
import { Pencil, ShoppingCart, Contact } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { DetailHeader } from '@/components/page/detail-header'
import { InfoRow } from '@/components/page/info-row'
import { useI18n } from '@/i18n/context'
import { formatDate } from '@/lib/format'
import type { Customer } from '@/services/customers-api'

function initials(name: string): string {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((p) => p[0] ?? '')
      .join('')
      .toUpperCase() || 'C'
  )
}

export default function CustomerShow({ customer }: { customer: Customer }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const a = customer.address
  const addressLine = a ? [a.street, a.city, a.postalCode, a.country].filter(Boolean).join(', ') : null

  return (
    <AuthenticatedLayout title={customer.name}>
      <Main>
        <div className="mx-auto grid max-w-3xl flex-1 items-start gap-6 md:gap-8">
          <DetailHeader backTo="/admin/customers" title={customer.name} subtitle={customer.email}>
            <Button variant="outline" onClick={() => navigate('/admin/orders/create')}>
              <ShoppingCart className="mr-2 h-4 w-4" />
              {t('customers_new_order_tooltip')}
            </Button>
            <Button onClick={() => navigate(`/admin/customers/${customer.id}/edit`)}>
              <Pencil className="mr-2 h-4 w-4" />
              {t('common_edit')}
            </Button>
          </DetailHeader>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Contact className="h-5 w-5" />
                {t('customers_show_profile_title')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-start gap-6">
                <Avatar className="h-16 w-16 shrink-0">
                  <AvatarFallback className="bg-primary/10 text-xl font-bold text-primary">
                    {initials(customer.name)}
                  </AvatarFallback>
                </Avatar>
                <dl className="flex-1 space-y-1">
                  <InfoRow label={t('customers_table_name')} value={customer.name} />
                  <Separator />
                  <InfoRow label={t('customers_table_email')} value={customer.email} />
                  <Separator />
                  <div className="flex items-center justify-between py-3">
                    <dt className="text-sm text-muted-foreground">{t('customers_table_address')}</dt>
                    <dd>
                      {addressLine ? (
                        <Badge variant="secondary">{addressLine}</Badge>
                      ) : (
                        <span className="text-sm text-muted-foreground">{t('customers_show_no_address')}</span>
                      )}
                    </dd>
                  </div>
                  <Separator />
                  <InfoRow label={t('common_created_at')} value={formatDate(customer.createdAt)} />
                </dl>
              </div>
            </CardContent>
          </Card>
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
