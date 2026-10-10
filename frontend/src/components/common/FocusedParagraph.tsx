// Adapted from TulevaEE/onboarding-client src/components/common/focusedParagraph (MIT, (c) 2017 Tuleva).
import { useEffect, useRef, type ReactNode } from 'react'

export function FocusedParagraph({
  className,
  children,
}: {
  className: string
  children: ReactNode
}) {
  const paragraph = useRef<HTMLParagraphElement>(null)
  useEffect(() => {
    if (document.activeElement === null || document.activeElement === document.body) {
      paragraph.current?.focus()
    }
  }, [])
  return (
    <p ref={paragraph} tabIndex={-1} className={className}>
      {children}
    </p>
  )
}
