// Adapted from TulevaEE/onboarding-client src/components/login/loginForm/IdCardLoginTab.tsx (MIT, (c) 2017 Tuleva).
import { FormattedMessage } from 'react-intl'

export function IdCardTab({ loading, onStart }: { loading: boolean; onStart: () => void }) {
  return (
    <div className="d-grid">
      <button type="button" className="btn btn-primary btn-lg" onClick={onStart} disabled={loading}>
        {loading && <span className="spinner-border spinner-border-sm me-2" role="status" />}
        <FormattedMessage id="login.enter" />
      </button>
    </div>
  )
}
