import { FormattedMessage } from 'react-intl'
import { LoginSuccess } from './LoginSuccess'
import { LoginTabs } from './LoginTabs'
import { PersonalCodeForm } from './PersonalCodeForm'
import { useLogin } from './useLogin'
import { VerificationCodeLoader } from './VerificationCodeLoader'

/**
 * Smart-ID and Mobile-ID, both emulated: the person types their personal code, sees a
 * verification code (greeted by name when the eID directory knows them), then is logged in.
 */
export function LoginPage() {
  const { state, start, cancel } = useLogin()

  const pending = state.phase === 'pending' ? state : null
  const errorAlert =
    state.phase === 'idle' && state.errorId ? (
      <div className="alert alert-danger text-start" role="alert">
        <FormattedMessage id={state.errorId} />
      </div>
    ) : null

  return (
    <div className="login-page mx-auto">
      <div className="bg-white shadow-sm rounded-3 p-4 p-sm-5 text-center border">
        {state.phase === 'success' ? (
          <LoginSuccess firstName={state.firstName} />
        ) : pending ? (
          <section
            aria-label={pending.method === 'smart-id' ? 'Smart-ID' : 'Mobiil-ID'}
            data-testid="pending-login"
          >
            <VerificationCodeLoader
              verificationCode={pending.verificationCode}
              firstName={pending.firstName}
              verifying={pending.verifying}
              onCancel={cancel}
            />
          </section>
        ) : (
          <>
            <h1 className="h2 m-0">
              <FormattedMessage id="login.title" />
            </h1>
            <p className="m-0 mt-2 text-body-secondary text-pretty">
              <FormattedMessage id="login.subtitle" />
            </p>
            <LoginTabs
              onTabChange={cancel}
              tabs={[
                {
                  labelId: 'login.smart.id',
                  content: (
                    <>
                      {errorAlert}
                      <PersonalCodeForm
                        id="smart-id"
                        withPhoneNumber={false}
                        onSubmit={(personalCode) => start('smart-id', { personalCode })}
                      />
                    </>
                  ),
                },
                {
                  labelId: 'login.mobile.id',
                  content: (
                    <>
                      {errorAlert}
                      <PersonalCodeForm
                        id="mobile-id"
                        withPhoneNumber
                        onSubmit={(personalCode, phoneNumber) =>
                          start('mobile-id', { personalCode, phoneNumber })
                        }
                      />
                    </>
                  ),
                },
              ]}
            />
            <p className="m-0 mt-4 text-body-secondary text-pretty small">
              <FormattedMessage id="login.permission.note" />
            </p>
          </>
        )}
      </div>
    </div>
  )
}
