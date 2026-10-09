import type { ReactNode } from 'react'

export function Placeholder({ children }: { children: ReactNode }) {
  return (
    <div className="card bg-gray-1 p-4 text-body-secondary text-center" role="note">
      {children}
    </div>
  )
}
