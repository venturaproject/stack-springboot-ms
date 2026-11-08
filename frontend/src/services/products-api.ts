import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'
import { listQuery } from '@/config/endpoints'
import type { Paginated } from '@/lib/api-utils'

/** Mirrors the backend `ProductResponse`. Money is integer minor units + ISO-4217 code. */
export interface Product {
  id: string
  sku: string
  name: string
  description: string | null
  priceAmountMinorUnits: number
  priceCurrency: string
  stockQuantity: number
  imageUrl: string | null
  createdAt: string | null
}

export interface CreateProductPayload {
  sku: string
  name: string
  description?: string | null
  priceAmountMinorUnits: number
  priceCurrency: string
  initialStock: number
}

export interface UpdateProductPayload {
  name?: string | null
  description?: string | null
  priceAmountMinorUnits?: number | null
  priceCurrency?: string | null
}

export const productsApi = {
  list(params?: { page?: number; size?: number; search?: string }): Promise<Paginated<Product>> {
    return axios.get(endpoints.products.list + listQuery(params)).then((r) => r.data)
  },

  detail(id: string): Promise<Product> {
    return axios.get(endpoints.products.detail(id)).then((r) => r.data)
  },

  create(payload: CreateProductPayload): Promise<Product> {
    return axios.post(endpoints.products.list, payload).then((r) => r.data)
  },

  update(id: string, payload: UpdateProductPayload): Promise<Product> {
    return axios.patch(endpoints.products.detail(id), payload).then((r) => r.data)
  },

  adjustStock(id: string, delta: number, reason?: string): Promise<Product> {
    return axios.post(endpoints.products.stock(id), { delta, reason }).then((r) => r.data)
  },

  uploadImage(id: string, file: File): Promise<Product> {
    const body = new FormData()
    body.append('image', file)
    return axios
      .post(endpoints.products.image(id), body, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then((r) => r.data)
  },

  removeImage(id: string): Promise<Product> {
    return axios.delete(endpoints.products.image(id)).then((r) => r.data)
  },
}

/** e.g. 1999 USD -> "$19.99" */
export function formatMoney(minorUnits: number, currency: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(minorUnits / 100)
}
