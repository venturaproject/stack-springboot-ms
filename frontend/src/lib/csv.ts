/**
 * Client-side CSV export — the bulk action for the data tables. The backend exposes no
 * export endpoint, so we build the file from the rows already loaded on the page.
 */
export interface CsvColumn<T> {
  header: string
  value: (row: T) => string | number | null | undefined
}

function escapeCell(value: string | number | null | undefined): string {
  const s = value == null ? '' : String(value)
  return /[",\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s
}

export function downloadCsv<T>(filename: string, rows: T[], columns: CsvColumn<T>[]): void {
  const header = columns.map((c) => escapeCell(c.header)).join(',')
  const body = rows.map((row) => columns.map((c) => escapeCell(c.value(row))).join(',')).join('\n')
  const blob = new Blob([`${header}\n${body}\n`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
