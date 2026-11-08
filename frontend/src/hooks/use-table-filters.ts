import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SEARCH_DEBOUNCE_MS } from '@/lib/constants'

interface UseTableFiltersOptions<T> {
  basePath: string
  initialFilters: T
  /** Rows per page — sent to the backend as `size`. */
  initialSize: number
  onNavigate?: () => void
}

function buildUrl(base: string, params: Record<string, string | number | boolean | null | undefined>): string {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  if (!entries.length) return base
  return base + '?' + new URLSearchParams(entries.map(([k, v]) => [k, String(v)])).toString()
}

export function useTableFilters<T extends Record<string, string | undefined>>({
  basePath,
  initialFilters,
  initialSize,
  onNavigate,
}: UseTableFiltersOptions<T>) {
  const navigate = useNavigate()
  const [filters, setFilters] = useState<T>(initialFilters)
  const [searchTerm, setSearchTerm] = useState(initialFilters?.search ?? '')
  const [size, setSize] = useState(initialSize)
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>(undefined)

  useEffect(() => () => clearTimeout(debounceTimer.current), [])

  const navigateWithFilters = useCallback((newFilters: T) => {
    setFilters(newFilters)
    onNavigate?.()
    navigate(buildUrl(basePath, newFilters as Record<string, string | number | boolean | null | undefined>), { replace: true })
  }, [basePath, onNavigate, navigate])

  const handleSearch = useCallback(
    (value: string) => {
      setSearchTerm(value)
      clearTimeout(debounceTimer.current)
      debounceTimer.current = setTimeout(() => {
        navigateWithFilters({ ...filters, search: value || undefined })
      }, SEARCH_DEBOUNCE_MS)
    },
    [filters, navigateWithFilters],
  )

  const handlePageChange = (page: number) => {
    navigate(buildUrl(basePath, { ...filters, page, size }), { replace: true })
  }

  const handleSizeChange = (value: string) => {
    const newSize = parseInt(value, 10)
    setSize(newSize)
    navigate(buildUrl(basePath, { ...filters, page: 1, size: newSize }), { replace: true })
  }

  return {
    filters,
    searchTerm,
    setSearchTerm,
    size,
    navigate: navigateWithFilters,
    handleSearch,
    handlePageChange,
    handleSizeChange,
  }
}
