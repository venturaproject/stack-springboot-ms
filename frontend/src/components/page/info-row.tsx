import type { ReactNode } from 'react'

/** One label / value row inside a detail card's `<dl>`, separated by `<Separator />`. */
export function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-right text-sm font-medium">{value || '—'}</dd>
    </div>
  )
}
