import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { MapPin } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { getErrorMessage } from '@/lib/errors'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import InputError from '@/components/InputError'
import { useI18n } from '@/i18n/context'
import { formatDate } from '@/lib/format'
import { customersApi, type Customer } from '@/services/customers-api'

const schema = z.object({
  street: z.string().min(1),
  city: z.string().min(1),
  postalCode: z.string().min(1),
  country: z.string().min(1),
})
type FormData = z.infer<typeof schema>

export default function CustomerEdit({ customer }: { customer: Customer }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      street: customer.address?.street ?? '',
      city: customer.address?.city ?? '',
      postalCode: customer.address?.postalCode ?? '',
      country: customer.address?.country ?? '',
    },
  })

  const onSubmit = async (data: FormData) => {
    try {
      await customersApi.updateAddress(customer.id, data)
      qc.invalidateQueries({ queryKey: ['customers'] })
      toast.success(t('customer_edit_success'))
      navigate(`/admin/customers/${customer.id}`)
    } catch (err) {
      const message = getErrorMessage(err, t('customer_edit_error'))
      setError('street', { message })
    }
  }

  const submit = handleSubmit(onSubmit)

  return (
    <AuthenticatedLayout title={t('customer_edit_title')}>
      <Main>
        <div className="grid flex-1 auto-rows-max items-start gap-4 md:gap-8">
          <FormHeader
            backTo="/admin/customers"
            title={t('customer_edit_title')}
            subtitle={customer.name}
            viewTo={`/admin/customers/${customer.id}`}
            cancelTo="/admin/customers"
            submitting={isSubmitting}
            submitLabel={t('common_save_changes')}
            onSubmit={submit}
          />

          <div className="grid gap-4 md:grid-cols-2 lg:gap-6">
            <Card className="h-full">
              <CardHeader>
                <CardTitle>{t('customer_edit_contact_title')}</CardTitle>
                <CardDescription>{t('customer_edit_contact_description')}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="grid gap-2">
                  <Label>{t('customers_table_name')}</Label>
                  <Input value={customer.name} disabled />
                </div>
                <div className="grid gap-2">
                  <Label>{t('customers_table_email')}</Label>
                  <Input value={customer.email} disabled />
                </div>
                <div className="grid gap-3 border-t pt-4 text-sm">
                  <div>
                    <p className="text-xs text-muted-foreground">{t('common_created_at')}</p>
                    <p className="mt-0.5 font-medium">{formatDate(customer.createdAt)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="h-full">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MapPin className="h-5 w-5" />
                  {t('customer_edit_address_title')}
                </CardTitle>
                <CardDescription>{t('customer_edit_card_description')}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="street">{t('customer_field_street')}</Label>
                  <Input id="street" {...register('street')} />
                  <InputError message={errors.street?.message} />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="grid gap-2">
                    <Label htmlFor="city">{t('customer_field_city')}</Label>
                    <Input id="city" {...register('city')} />
                    <InputError message={errors.city?.message} />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="postalCode">{t('customer_field_postal_code')}</Label>
                    <Input id="postalCode" {...register('postalCode')} />
                    <InputError message={errors.postalCode?.message} />
                  </div>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="country">{t('customer_field_country')}</Label>
                  <Input id="country" {...register('country')} />
                  <InputError message={errors.country?.message} />
                </div>
              </CardContent>
            </Card>
          </div>

          <FormMobileActions
            cancelTo="/admin/customers"
            submitting={isSubmitting}
            submitLabel={t('common_save_changes')}
            onSubmit={submit}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
