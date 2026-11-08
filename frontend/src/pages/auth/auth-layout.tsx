import { BrandMark } from '@/components/brand-mark'
import { DocumentTitle } from '@/components/document-title'
import { appName } from '@/lib/app-config'

interface Props {
  children: React.ReactNode
  title?: string
}

export default function AuthLayout({ children, title = appName }: Props) {
  return (
    <>
      <DocumentTitle title={title} />
      <div className='container grid h-svh flex-col items-center justify-center bg-primary-foreground lg:max-w-none lg:px-0'>
        <div className='mx-auto flex w-full flex-col justify-center space-y-2 sm:w-[480px] lg:p-8'>
          <div className='mb-4 flex items-center justify-center gap-3'>
            <BrandMark />
            <h1 className='text-xl font-medium'>{appName}</h1>
          </div>
          {children}
        </div>
      </div>
    </>
  )
}
