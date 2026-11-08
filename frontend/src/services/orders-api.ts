import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'
import { listQuery } from '@/config/endpoints'
import type { Paginated } from '@/lib/api-utils'
import type { Address } from '@/services/customers-api'

export type OrderStatus = 'DRAFT' | 'PLACED' | 'CONFIRMED' | 'CANCELLED'

/** Mirrors the backend `OrderLineResponse`. */
export interface OrderLine {
  id: string
  productId: string
  productSku: string
  productName: string
  unitPriceAmountMinorUnits: number
  currency: string
  quantity: number
  lineTotalMinorUnits: number
}

/** Mirrors the backend `OrderSummaryResponse`. */
export interface OrderSummary {
  id: string
  customerId: string
  status: OrderStatus
  totalAmountMinorUnits: number | null
  currency: string | null
  createdAt: string
}

/** Mirrors the backend `OrderResponse` (summary + lines + shipping address). */
export interface Order extends OrderSummary {
  shippingAddress: Address | null
  lines: OrderLine[]
}

export interface AddOrderLinePayload {
  orderId?: string | null
  customerId?: string | null
  productId: string
  quantity: number
}

export const ordersApi = {
  list(params?: {
    page?: number
    size?: number
    customerId?: string
    status?: OrderStatus
  }): Promise<Paginated<OrderSummary>> {
    return axios.get(endpoints.orders.list + listQuery(params)).then((r) => r.data)
  },

  detail(id: string): Promise<Order> {
    return axios.get(endpoints.orders.detail(id)).then((r) => r.data)
  },

  addLine(payload: AddOrderLinePayload): Promise<Order> {
    return axios.post(endpoints.orders.lines, payload).then((r) => r.data)
  },

  updateLineQuantity(id: string, lineId: string, quantity: number): Promise<Order> {
    return axios.patch(endpoints.orders.line(id, lineId), { quantity }).then((r) => r.data)
  },

  removeLine(id: string, lineId: string): Promise<Order> {
    return axios.delete(endpoints.orders.line(id, lineId)).then((r) => r.data)
  },

  place(id: string): Promise<Order> {
    return axios.post(endpoints.orders.place(id)).then((r) => r.data)
  },

  cancel(id: string): Promise<Order> {
    return axios.post(endpoints.orders.cancel(id)).then((r) => r.data)
  },
}
