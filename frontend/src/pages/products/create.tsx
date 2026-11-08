import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Package, ImagePlus, X } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { getErrorMessage } from '@/lib/errors'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import InputError from '@/components/InputError'
import { useI18n } from '@/i18n/context'
import { productsApi } from '@/services/products-api'

const schema = z.object({
  sku: z.string().regex(/^[A-Za-z0-9]{3}-[A-Za-z0-9]{4,}$/, 'ABC-1234'),
  name: z.string().min(1).max(255),
  description: z.string().max(5000).optional(),
  price: z.coerce.number().min(0),
  currency: z.enum(['USD', 'EUR']),
  initialStock: z.coerce.number().int().min(0),
})
type FormInput = z.input<typeof schema>
type FormData = z.output<typeof schema>

export default function ProductCreate() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
    defaultValues: { sku: '', name: '', description: '', price: 0, currency: 'USD', initialStock: 0 },
  })

  const pickImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImageFile(file)
    setImagePreview(URL.createObjectURL(file))
  }

  const onSubmit = async (data: FormData) => {
    try {
      const created = await productsApi.create({
        sku: data.sku.toUpperCase(),
        name: data.name,
        description: data.description || null,
        priceAmountMinorUnits: Math.round(data.price * 100),
        priceCurrency: data.currency,
        initialStock: data.initialStock,
      })
      if (imageFile) {
        try {
          await productsApi.uploadImage(created.id, imageFile)
        } catch {
          toast.error(t('product_image_upload_error'))
        }
      }
      qc.invalidateQueries({ queryKey: ['products'] })
      toast.success(t('product_create_success'))
      navigate(`/admin/products/${created.id}`)
    } catch (err) {
      const message = getErrorMessage(err, t('product_create_error'))
      setError('sku', { message })
    }
  }

  const selectClass = 'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm'
  const submit = handleSubmit(onSubmit)

  return (
    <AuthenticatedLayout title={t('product_create_title')}>
      <Main>
        <div className="mx-auto grid w-full max-w-2xl flex-1 items-start gap-4 md:gap-6">
          <FormHeader
            backTo="/admin/products"
            title={t('product_create_title')}
            cancelTo="/admin/products"
            submitting={isSubmitting}
            submitLabel={t('create_product')}
            onSubmit={submit}
          />

          <Card>
            <CardHeader className="items-center text-center">
              <div className="mb-1 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Package className="h-6 w-6" />
              </div>
              <CardTitle>{t('product_create_card_title')}</CardTitle>
              <CardDescription>{t('product_create_card_description')}</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="grid gap-2">
                <Label>{t('product_field_image')}</Label>
                <div className="flex items-center gap-4">
                  <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted">
                    {imagePreview ? (
                      <img src={imagePreview} alt="" className="h-full w-full object-cover" />
                    ) : (
                      <Package className="h-8 w-8 text-muted-foreground" />
                    )}
                  </div>
                  <div className="flex flex-col gap-2">
                    <Button type="button" variant="outline" size="sm" onClick={() => fileRef.current?.click()}>
                      <ImagePlus className="mr-2 h-4 w-4" />
                      {t('product_field_image_upload')}
                    </Button>
                    {imagePreview && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setImageFile(null)
                          setImagePreview(null)
                          if (fileRef.current) fileRef.current.value = ''
                        }}
                      >
                        <X className="mr-2 h-4 w-4" />
                        {t('common_delete')}
                      </Button>
                    )}
                    <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={pickImage} />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="sku">{t('products_table_sku')}</Label>
                  <Input id="sku" placeholder="TSH-0001" {...register('sku')} />
                  <InputError message={errors.sku?.message} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="initialStock">{t('product_field_initial_stock')}</Label>
                  <Input id="initialStock" type="number" min="0" {...register('initialStock')} />
                  <InputError message={errors.initialStock?.message} />
                </div>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="name">{t('products_table_name')}</Label>
                <Input id="name" {...register('name')} />
                <InputError message={errors.name?.message} />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="description">{t('description')}</Label>
                <Textarea id="description" rows={3} {...register('description')} />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="price">{t('products_table_price')}</Label>
                  <Input id="price" type="number" min="0" step="0.01" {...register('price')} />
                  <InputError message={errors.price?.message} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="currency">{t('product_field_currency')}</Label>
                  <select id="currency" className={selectClass} {...register('currency')}>
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>

          <FormMobileActions
            cancelTo="/admin/products"
            submitting={isSubmitting}
            submitLabel={t('create_product')}
            onSubmit={submit}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
