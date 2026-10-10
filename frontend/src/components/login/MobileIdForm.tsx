// Adapted from TulevaEE/onboarding-client src/components/login/mobileId/MobileIdLoginForm.tsx (MIT, (c) 2017 Tuleva).
import { useState, type FormEvent } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { normalizeMobileIdPhoneNumber } from './mobileIdPhoneNumber'
import { isValidPersonalCode } from './personalCode'

const PERSONAL_CODE_LENGTH = 11

const phoneNumberProblemMessages = {
  NOT_ESTONIAN: 'login.mobile.id.phone.number.not.estonian',
  INVALID: 'login.mobile.id.phone.number.check',
} as const

export function MobileIdForm({
  onSubmit,
}: {
  onSubmit: (personalCode: string, phoneNumber: string) => void
}) {
  const { formatMessage } = useIntl()
  const [personalCode, setPersonalCode] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [submittedInvalidCode, setSubmittedInvalidCode] = useState(false)
  const [numberProblem, setNumberProblem] = useState<'NOT_ESTONIAN' | 'INVALID' | null>(null)

  const personalCodeValid = isValidPersonalCode(personalCode)
  const showPersonalCodeError =
    !personalCodeValid && (personalCode.length >= PERSONAL_CODE_LENGTH || submittedInvalidCode)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!personalCodeValid) {
      setSubmittedInvalidCode(true)
      return
    }
    const normalized = normalizeMobileIdPhoneNumber(phoneNumber)
    if ('problem' in normalized) {
      setNumberProblem(normalized.problem)
      return
    }
    onSubmit(personalCode, normalized.phoneNumber)
  }

  return (
    <form onSubmit={submit} noValidate>
      <div className="mb-3">
        <input
          id="mobile-id-personal-code"
          type="text"
          inputMode="numeric"
          autoComplete="username"
          value={personalCode}
          onChange={(event) => {
            setPersonalCode(event.target.value)
            setSubmittedInvalidCode(false)
          }}
          className={`form-control form-control-lg${showPersonalCodeError ? ' is-invalid' : ''}`}
          placeholder={formatMessage({ id: 'login.id.code' })}
          aria-label={formatMessage({ id: 'login.id.code' })}
          aria-invalid={showPersonalCodeError}
          aria-describedby={showPersonalCodeError ? 'mobile-id-personal-code-error' : undefined}
        />
        {showPersonalCodeError && (
          <div id="mobile-id-personal-code-error" className="invalid-feedback text-start">
            <FormattedMessage id="login.mobile.id.personal.code.invalid" />
          </div>
        )}
      </div>
      <div className="mb-3">
        <input
          id="mobile-id-number"
          type="tel"
          autoComplete="tel"
          value={phoneNumber}
          onChange={(event) => {
            setPhoneNumber(event.target.value)
            setNumberProblem(null)
          }}
          className={`form-control form-control-lg${numberProblem ? ' is-invalid' : ''}`}
          placeholder={formatMessage({ id: 'login.phone.number' })}
          aria-label={formatMessage({ id: 'login.phone.number' })}
          aria-invalid={numberProblem !== null}
          aria-describedby={numberProblem ? 'mobile-id-number-error' : undefined}
        />
        {numberProblem && (
          <div id="mobile-id-number-error" className="invalid-feedback text-start">
            <FormattedMessage id={phoneNumberProblemMessages[numberProblem]} />
          </div>
        )}
      </div>
      <div className="d-grid mb-3">
        <button
          type="submit"
          className="btn btn-primary btn-lg"
          disabled={!personalCode || !phoneNumber}
        >
          <FormattedMessage id="login.enter" />
        </button>
      </div>
    </form>
  )
}
