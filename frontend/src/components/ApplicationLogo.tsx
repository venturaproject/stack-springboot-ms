import { SVGAttributes } from 'react'

export default function ApplicationLogo(props: SVGAttributes<SVGElement>) {
  return (
    <svg {...props} viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" fill="none">
      <rect x="8" y="8" width="20" height="20" rx="3" stroke="currentColor" strokeWidth="4" />
      <rect x="36" y="8" width="20" height="20" rx="3" stroke="currentColor" strokeWidth="4" />
      <rect x="8" y="36" width="20" height="20" rx="3" stroke="currentColor" strokeWidth="4" />
      <rect x="36" y="36" width="20" height="20" rx="3" stroke="currentColor" strokeWidth="4" />
    </svg>
  )
}
