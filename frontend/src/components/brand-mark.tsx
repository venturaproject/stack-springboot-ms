import { cn } from '@/lib/utils'

type BrandMarkProps = { className?: string; iconClassName?: string }

export function BrandMark({ className, iconClassName }: BrandMarkProps) {
  return (
    <span
      className={cn(
        'relative inline-flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-gradient-to-br from-primary via-primary to-violet-600 text-primary-foreground shadow-lg shadow-primary/25 ring-1 ring-white/20',
        className,
      )}
      aria-hidden="true"
    >
      <svg className={cn('size-7', iconClassName)} viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M16 3.5 27 9.75v12.5L16 28.5 5 22.25V9.75L16 3.5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="m5.5 10 10.5 6 10.5-6M16 16v12" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="m16 8 3 1.75v3.5L16 15l-3-1.75v-3.5L16 8Z" fill="currentColor" fillOpacity=".9" />
      </svg>
      <span className="absolute -right-1 -top-1 size-4 rounded-full bg-cyan-300/80 blur-sm" />
    </span>
  )
}
