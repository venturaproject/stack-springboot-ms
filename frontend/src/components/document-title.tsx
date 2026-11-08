import { useEffect } from 'react'
import { appName } from '@/lib/app-config'

export function DocumentTitle({ title }: { title?: string }) {
  useEffect(() => {
    document.title = title ? `${title} - ${appName}` : appName
  }, [title])

  return null
}
