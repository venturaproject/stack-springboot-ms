import type { ReactNode } from 'react'
import { ChevronLeft } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'

/**
 * Header for a detail / "show" page: a back button, the title + optional monospace or
 * muted subtitle, and a slot for action buttons on the right. Matches the layout used
 * across every detail page.
 */
export function DetailHeader({
  backTo,
  title,
  subtitle,
  subtitleMono = false,
  children,
}: {
  backTo: string
  title: string
  subtitle?: string
  subtitleMono?: boolean
  children?: ReactNode
}) {
  const navigate = useNavigate()
  return (
    <div className="flex items-center gap-4">
      <Button variant="outline" size="icon" onClick={() => navigate(backTo)}>
        <ChevronLeft className="h-4 w-4" />
      </Button>
      <div className="flex-1">
        <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
        {subtitle && (
          <p className={`text-sm text-muted-foreground${subtitleMono ? ' font-mono' : ''}`}>{subtitle}</p>
        )}
      </div>
      {children}
    </div>
  )
}
