import { useState } from 'react'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import NotificationSlideout from '@/components/notification/notification-slideout'
import { useNotifications } from '@/hooks/use-notifications'

const NotificationButton = () => {
  const [isOpen, setIsOpen] = useState(false)
  const { unreadCount } = useNotifications()

  return (
    <>
      <Button variant="ghost" size="icon" onClick={() => setIsOpen((v) => !v)} className="relative">
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1.5 top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-medium text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </Button>
      <NotificationSlideout isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </>
  )
}

export default NotificationButton
