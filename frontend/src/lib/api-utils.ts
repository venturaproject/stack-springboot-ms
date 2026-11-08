/**
 * Backend response helpers.
 *
 * The backend is the source of truth for shapes. `Paginated<T>` mirrors the Spring
 * `PagedResponse` record field-for-field — there is no translation layer.
 */

export interface Paginated<T> {
  data: T[]
  page: number
  perPage: number
  count: number
  totalPages: number
}

export function emptyPage<T>(): Paginated<T> {
  return { data: [], page: 1, perPage: 20, count: 0, totalPages: 1 }
}

/**
 * Normalises a list response into {@link Paginated}. Accepts either a `PagedResponse`
 * (paginated endpoints like `/api/products`, `/api/users`) or a plain array (endpoints
 * that return the full collection, like `/api/roles`, `/api/permissions`).
 */
export function toPaginated<T>(raw: unknown): Paginated<T> {
  if (Array.isArray(raw)) {
    return { data: raw as T[], page: 1, perPage: raw.length || 20, count: raw.length, totalPages: 1 }
  }
  if (raw && typeof raw === 'object' && Array.isArray((raw as { data?: unknown }).data)) {
    const r = raw as Partial<Paginated<T>>
    return {
      data: r.data ?? [],
      page: r.page ?? 1,
      perPage: r.perPage ?? (r.data?.length || 20),
      count: r.count ?? r.data?.length ?? 0,
      totalPages: r.totalPages ?? 1,
    }
  }
  return emptyPage<T>()
}

/** Unwrap a detail response — the backend returns the entity directly. */
export function unwrap<T>(raw: unknown): T | null {
  if (!raw || typeof raw !== 'object') return null
  const r = raw as { data?: unknown }
  return (r.data && typeof r.data === 'object' ? r.data : raw) as T
}
