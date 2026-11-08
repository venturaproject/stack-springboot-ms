import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Contact } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { getErrorMessage } from '@/lib/errors'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import InputError from '@/components/InputError'
import { useI18n } from '@/i18n/context'
import { customersApi } from '@/services/customers-api'

const schema = z
  .object({
    name: z.string().min(1).max(255),
    email: z.string().email(),
    street: z.string().optional(),
    city: z.string().optional(),
    postalCode: z.string().optional(),
    country: z.string().optional(),
  })
  .refine(
    (d) => {
      const parts = [d.street, d.city, d.postalCode, d.country].map((v) => v?.trim() ?? '')
      return parts.every((p) => p === '') || parts.every((p) => p !== '')
    },
    { message: 'address_all_or_none', path: ['street'] },
  )
type FormData = z.infer<typeof schema>

export default function CustomerCreate() {
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
    defaultValues: { name: '', email: '', street: '', city: '', postalCode: '', country: '' },
  })

  const onSubmit = async (data: FormData) => {
    try {
      const customer = await customersApi.register({
        name: data.name,
        email: data.email,
        addressStreet: data.street || null,
        addressCity: data.city || null,
        addressPostalCode: data.postalCode || null,
        addressCountry: data.country || null,
      })
      qc.invalidateQueries({ queryKey: ['customers'] })
      toast.success(t('customer_create_success'))
      navigate(`/admin/customers/${customer.id}`)
    } catch (err) {
      const message = getErrorMessage(err, t('customer_create_error'))
      setError('email', { message })
    }
  }

  const submit = handleSubmit(onSubmit)

  return (
    <AuthenticatedLayout title={t('customer_create_title')}>
      <Main>
        <div className="mx-auto grid w-full max-w-2xl flex-1 items-start gap-4 md:gap-6">
          <FormHeader
            backTo="/admin/customers"
            title={t('customer_create_title')}
            cancelTo="/admin/customers"
            submitting={isSubmitting}
            submitLabel={t('customer_create_submit')}
            onSubmit={submit}
          />

          <Card>
            <CardHeader className="items-center text-center">
              <div className="mb-1 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Contact className="h-6 w-6" />
              </div>
              <CardTitle>{t('customer_create_card_title')}</CardTitle>
              <CardDescription>{t('customer_create_card_description')}</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="name">{t('customers_table_name')}</Label>
                  <Input id="name" {...register('name')} placeholder="Ada Lovelace" />
                  <InputError message={errors.name?.message} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="email">{t('customers_table_email')}</Label>
                  <Input id="email" type="email" {...register('email')} placeholder="ada@example.com" />
                  <InputError message={errors.email?.message} />
                </div>
              </div>

              <div className="grid gap-2 rounded-md border p-4">
                <CardDescription>{t('customer_address_optional_hint')}</CardDescription>
                <Input placeholder={t('customer_field_street')} {...register('street')} />
                <div className="grid grid-cols-3 gap-3">
                  <Input placeholder={t('customer_field_city')} {...register('city')} />
                  <Input placeholder={t('customer_field_postal_code')} {...register('postalCode')} />
                  <Input placeholder={t('customer_field_country')} {...register('country')} />
                </div>
                <InputError message={errors.street?.message} />
              </div>
            </CardContent>
          </Card>

          <FormMobileActions
            cancelTo="/admin/customers"
            submitting={isSubmitting}
            submitLabel={t('customer_create_submit')}
            onSubmit={submit}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
