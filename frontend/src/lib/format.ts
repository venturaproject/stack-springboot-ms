export { formatMoney } from '@/services/products-api'

/** Locale-aware short date, e.g. "Aug 31, 2026". */
export function formatDate(value: string | number | Date): string {
  return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

/** Locale-aware date + time. */
export function formatDateTime(value: string | number | Date): string {
  return new Date(value).toLocaleString()
}
