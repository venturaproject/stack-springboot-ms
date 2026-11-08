import { useEffect, useRef, useState } from 'react'

/**
 * The storage key is scoped to the route AND the current column set, so changing a
 * table's columns (adding, removing, renaming) starts from a clean order instead of
 * inheriting a stale one — that's what made row actions land mid-row after a rework.
 * A pure reorder keeps the same key (the canonical id list is unchanged) so it persists.
 */
function keyFor(prefix: string, columnIds: string[]) {
  return `${prefix}:${window.location.pathname}:${columnIds.join(',')}`
}

function readStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

/** `select` first, `actions` last, whatever the drag history. */
function pinFixed(order: string[], present: string[]): string[] {
  const known = new Set(present)
  const middle = order.filter((id) => known.has(id) && id !== 'select' && id !== 'actions')
  const missing = present.filter((id) => !order.includes(id) && id !== 'select' && id !== 'actions')
  return [
    ...(known.has('select') ? ['select'] : []),
    ...middle,
    ...missing,
    ...(known.has('actions') ? ['actions'] : []),
  ]
}

/**
 * Column order + visibility with per-table localStorage persistence and drag-to-reorder.
 *
 * @param initialIds - the canonical column order (fallback + storage-key seed)
 */
export function useColumnReorder(initialIds: string[]) {
  const orderKey = keyFor('column-order', initialIds)
  const visibilityKey = keyFor('column-visibility', initialIds)

  const [columnOrder, setColumnOrder] = useState<string[]>(() =>
    pinFixed(readStorage(orderKey, initialIds), initialIds),
  )
  const [columnVisibility, setColumnVisibility] = useState<Record<string, boolean>>(() =>
    readStorage(visibilityKey, {}),
  )
  const dragColumnId = useRef<string | null>(null)

  useEffect(() => {
    try {
      localStorage.setItem(orderKey, JSON.stringify(columnOrder))
    } catch {
      // localStorage full or blocked (private mode) — order just won't persist
    }
  }, [columnOrder, orderKey])

  useEffect(() => {
    try {
      localStorage.setItem(visibilityKey, JSON.stringify(columnVisibility))
    } catch {
      // see above
    }
  }, [columnVisibility, visibilityKey])

  const handleDragStart = (id: string) => {
    dragColumnId.current = id
  }

  const handleDrop = (targetId: string) => {
    if (!dragColumnId.current || dragColumnId.current === targetId) return
    setColumnOrder((prev) => {
      const next = [...prev]
      const from = next.indexOf(dragColumnId.current!)
      const to = next.indexOf(targetId)
      next.splice(from, 1)
      next.splice(to, 0, dragColumnId.current!)
      return pinFixed(next, next)
    })
    dragColumnId.current = null
  }

  return {
    columnOrder,
    columnVisibility,
    setColumnVisibility,
    handleDragStart,
    handleDrop,
  }
}
