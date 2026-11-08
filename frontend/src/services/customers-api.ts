import { axios } from '@/lib/axios'
import { endpoints } from '@/lib/endpoints'
import { listQuery } from '@/config/endpoints'
import type { Paginated } from '@/lib/api-utils'

/** Mirrors the backend `AddressResponse`. */
export interface Address {
  street: string
  city: string
  postalCode: string
  country: string
}

/** Mirrors the backend `CustomerResponse`. */
export interface Customer {
  id: string
  name: string
  email: string
  address: Address | null
  createdAt: string
}

/** Mirrors the backend `CustomerListResponse` — a paginated envelope plus header stats. */
export interface CustomerListPage extends Paginated<Customer> {
  stats: { total: number; withAddress: number; withoutAddress: number }
}

export interface RegisterCustomerPayload {
  name: string
  email: string
  addressStreet?: string | null
  addressCity?: string | null
  addressPostalCode?: string | null
  addressCountry?: string | null
}

export const customersApi = {
  list(params?: {
    page?: number
    size?: number
    search?: string
    hasAddress?: boolean
  }): Promise<CustomerListPage> {
    return axios.get(endpoints.customers.list + listQuery(params)).then((r) => r.data)
  },

  detail(id: string): Promise<Customer> {
    return axios.get(endpoints.customers.detail(id)).then((r) => r.data)
  },

  register(payload: RegisterCustomerPayload): Promise<Customer> {
    return axios.post(endpoints.customers.list, payload).then((r) => r.data)
  },

  updateAddress(id: string, address: Address): Promise<Customer> {
    return axios.patch(endpoints.customers.address(id), address).then((r) => r.data)
  },
}
