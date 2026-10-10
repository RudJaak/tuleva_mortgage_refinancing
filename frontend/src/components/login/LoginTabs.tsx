// Adapted from TulevaEE/onboarding-client src/components/login/loginForm/LoginTabs.js and LoginTab.js
// (MIT, (c) 2017 Tuleva).
import { useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { FormattedMessage } from 'react-intl'

export type LoginTab = { labelId: string; hideOnMobile?: boolean; content: ReactNode }

const PANEL_ID = 'login-tab-panel'
const tabId = (labelId: string) => `${labelId.replace(/\./g, '-')}-tab`

const indexAfterKey: Record<string, (index: number, count: number) => number> = {
  ArrowRight: (index, count) => (index + 1) % count,
  ArrowLeft: (index, count) => (index - 1 + count) % count,
  Home: () => 0,
  End: (_index, count) => count - 1,
}

export function LoginTabs({ tabs, onTabChange }: { tabs: LoginTab[]; onTabChange: () => void }) {
  const [active, setActive] = useState(tabs[0].labelId)
  const buttons = useRef(new Map<string, HTMLButtonElement | null>())

  const select = (labelId: string) => {
    if (labelId !== active) {
      onTabChange()
    }
    setActive(labelId)
  }

  const onKeyDown = (event: KeyboardEvent<HTMLUListElement>) => {
    const next = indexAfterKey[event.key]
    if (!next) {
      return
    }
    event.preventDefault()
    const focused = tabs.findIndex((tab) => buttons.current.get(tab.labelId) === event.target)
    buttons.current.get(tabs[next(focused, tabs.length)].labelId)?.focus()
  }

  return (
    <>
      <ul
        className="mt-4 pt-2 mt-sm-5 pt-sm-0 mb-4 nav nav-tabs nav-fill"
        role="tablist"
        onKeyDown={onKeyDown}
      >
        {tabs.map((tab) => {
          const selected = tab.labelId === active
          return (
            <li
              key={tab.labelId}
              role="presentation"
              className={`nav-item ${tab.hideOnMobile ? 'd-none d-md-block' : ''}`}
            >
              <button
                type="button"
                role="tab"
                id={tabId(tab.labelId)}
                aria-selected={selected}
                aria-controls={PANEL_ID}
                tabIndex={selected ? 0 : -1}
                ref={(button) => {
                  buttons.current.set(tab.labelId, button)
                }}
                className={`nav-link ${selected ? 'active' : ''}`}
                onClick={() => select(tab.labelId)}
              >
                <FormattedMessage id={tab.labelId} />
              </button>
            </li>
          )
        })}
      </ul>
      <div
        className="tab-content pt-2"
        id={PANEL_ID}
        role="tabpanel"
        aria-labelledby={tabId(active)}
        aria-live="polite"
      >
        {tabs.find((tab) => tab.labelId === active)?.content}
      </div>
    </>
  )
}
