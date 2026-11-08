import { appName } from '@/lib/app-config'
import { BrandMark } from '@/components/brand-mark'

const AppLogo = ({ className = '', isCollapsed = false }) => {
  return (
    <span
      className={`inline-flex h-10 items-center gap-3 text-foreground ${className}`}
      aria-label={appName}
    >
      <BrandMark className="size-9 rounded-xl" iconClassName="size-6" />
      {!isCollapsed && <span className="truncate font-semibold">{appName}</span>}
    </span>
  )
}

export default AppLogo
