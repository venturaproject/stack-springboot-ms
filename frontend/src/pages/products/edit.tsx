import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Package, ImagePlus, Trash2 } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { getErrorMessage } from '@/lib/errors'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { FormHeader, FormMobileActions } from '@/components/page/form-header'
import InputError from '@/components/InputError'
import { useI18n } from '@/i18n/context'
import { formatDate } from '@/lib/format'
import { productsApi, type Product } from '@/services/products-api'

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(5000).optional(),
  price: z.coerce.number().min(0),
  currency: z.enum(['USD', 'EUR']),
})
type FormInput = z.input<typeof schema>
type FormData = z.output<typeof schema>

export default function ProductEdit({ product }: { product: Product }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)

  const [stockQuantity, setStockQuantity] = useState(product.stockQuantity)
  const [stockTarget, setStockTarget] = useState(String(product.stockQuantity))
  const [imageUrl, setImageUrl] = useState(product.imageUrl)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [imageBusy, setImageBusy] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: product.name,
      description: product.description ?? '',
      price: product.priceAmountMinorUnits / 100,
      currency: (product.priceCurrency as 'USD' | 'EUR') ?? 'USD',
    },
  })

  const uploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImagePreview(URL.createObjectURL(file))
    setImageBusy(true)
    try {
      const updated = await productsApi.uploadImage(product.id, file)
      setImageUrl(updated.imageUrl)
      qc.invalidateQueries({ queryKey: ['products'] })
      toast.success(t('product_image_upload_success'))
    } catch {
      toast.error(t('product_image_upload_error'))
    } finally {
      setImagePreview(null)
      setImageBusy(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const removeImage = async () => {
    setImageBusy(true)
    try {
      await productsApi.removeImage(product.id)
      setImageUrl(null)
      qc.invalidateQueries({ queryKey: ['products'] })
    } catch {
      toast.error(t('product_image_upload_error'))
    } finally {
      setImageBusy(false)
    }
  }

  const onSubmit = async (data: FormData) => {
    try {
      await productsApi.update(product.id, {
        name: data.name,
        description: data.description || null,
        priceAmountMinorUnits: Math.round(data.price * 100),
        priceCurrency: data.currency,
      })
      const target = Number(stockTarget)
      const delta = Number.isInteger(target) ? target - stockQuantity : 0
      if (delta !== 0) {
        const updated = await productsApi.adjustStock(product.id, delta, 'manual_adjustment')
        setStockQuantity(updated.stockQuantity)
        setStockTarget(String(updated.stockQuantity))
      }
      qc.invalidateQueries({ queryKey: ['products'] })
      toast.success(t('product_edit_success'))
      navigate(`/admin/products/${product.id}`)
    } catch (err) {
      const message = getErrorMessage(err, t('product_edit_error'))
      setError('name', { message })
    }
  }

  const src = imagePreview ?? imageUrl
  const selectClass = 'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm'
  const submit = handleSubmit(onSubmit)

  return (
    <AuthenticatedLayout title={t('product_edit_title')}>
      <Main>
        <div className="grid flex-1 auto-rows-max items-start gap-4 md:gap-8">
          <FormHeader
            backTo="/admin/products"
            title={t('product_edit_title')}
            subtitle={product.sku}
            subtitleMono
            viewTo={`/admin/products/${product.id}`}
            cancelTo="/admin/products"
            submitting={isSubmitting}
            submitLabel={t('common_save_changes')}
            onSubmit={submit}
          />

          <div className="grid gap-4 md:grid-cols-2 lg:gap-6">
            <Card className="h-full">
              <CardHeader>
                <CardTitle>{t('product_edit_card_title')}</CardTitle>
                <CardDescription>{t('product_edit_card_description')}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="grid gap-2">
                  <Label>{t('product_field_image')}</Label>
                  <div className="flex items-center gap-4">
                    <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted">
                      {src ? (
                        <img src={src} alt={product.name} className="h-full w-full object-cover" />
                      ) : (
                        <Package className="h-8 w-8 text-muted-foreground" />
                      )}
                    </div>
                    <div className="flex flex-col gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={imageBusy}
                        onClick={() => fileRef.current?.click()}
                      >
                        <ImagePlus className="mr-2 h-4 w-4" />
                        {t('product_field_image_upload')}
                      </Button>
                      {imageUrl && (
                        <Button type="button" variant="ghost" size="sm" disabled={imageBusy} onClick={removeImage}>
                          <Trash2 className="mr-2 h-4 w-4" />
                          {t('common_delete')}
                        </Button>
                      )}
                      <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={uploadImage} />
                    </div>
                  </div>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="name">{t('products_table_name')}</Label>
                  <Input id="name" {...register('name')} />
                  <InputError message={errors.name?.message} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="description">{t('description')}</Label>
                  <Textarea id="description" rows={4} {...register('description')} />
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

            <Card className="h-full">
              <CardHeader>
                <CardTitle>{t('product_edit_stock_title')}</CardTitle>
                <CardDescription>{t('product_edit_stock_description')}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-5">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">{t('products_table_stock')}</span>
                  <Badge variant={stockQuantity <= 5 ? 'destructive' : 'secondary'}>{stockQuantity}</Badge>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="stock">{t('product_edit_stock_quantity_label')}</Label>
                  <Input id="stock" type="number" value={stockTarget} onChange={(e) => setStockTarget(e.target.value)} />
                </div>
                <div className="grid gap-3 border-t pt-4 text-sm">
                  <div>
                    <p className="text-xs text-muted-foreground">{t('common_created_at')}</p>
                    <p className="mt-0.5 font-medium">{product.createdAt ? formatDate(product.createdAt) : '—'}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <FormMobileActions
            cancelTo="/admin/products"
            submitting={isSubmitting}
            submitLabel={t('common_save_changes')}
            onSubmit={submit}
          />
        </div>
      </Main>
    </AuthenticatedLayout>
  )
}
