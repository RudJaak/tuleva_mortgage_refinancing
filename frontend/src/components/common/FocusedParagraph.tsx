// Adapted from TulevaEE/onboarding-client src/components/common/focusedParagraph (MIT, (c) 2017 Tuleva).
import { useEffect, useRef, type ReactNode } from 'react'

export function FocusedParagraph({
  className,
  children,
}: {
  className: string
  children: ReactNode
}) {
  // Focus moves here so screen readers announce the new step; the paragraph is not interactive,
  // so it gets no focus ring (.focused-paragraph)
  const paragraph = useRef<HTMLParagraphElement>(null)
  useEffect(() => {
    if (document.activeElement === null || document.activeElement === document.body) {
      paragraph.current?.focus()
    }
  }, [])
  return (
    <p ref={paragraph} tabIndex={-1} className={`focused-paragraph ${className}`}>
      {children}
    </p>
  )
}
