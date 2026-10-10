import { FormattedMessage } from 'react-intl'
import { MobileIdForm } from './MobileIdForm'
import { IdCardTab } from './IdCardTab'
import { LoginSuccess } from './LoginSuccess'
import { LoginTabs } from './LoginTabs'
import { SmartIdQrLogin } from './SmartIdQrLogin'
import { SmartIdTab } from './SmartIdTab'
import { useLogin } from './useLogin'
import { VerificationCodeLoader } from './VerificationCodeLoader'

export function LoginPage() {
  const { state, start, cancel } = useLogin()

  const pending = state.phase === 'pending' ? state : null
  const confirmingOnThePhone = pending && pending.method !== 'id-card'
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
        ) : confirmingOnThePhone ? (
          <section
            aria-label={pending.method === 'smart-id' ? 'Smart-ID' : 'Mobiil-ID'}
            data-testid="pending-login"
          >
            {pending.method === 'smart-id' ? (
              <SmartIdQrLogin deviceLink={pending.deviceLink} onCancel={cancel} />
            ) : (
              <VerificationCodeLoader
                verificationCode={pending.verificationCode}
                verifying={pending.verifying}
                onCancel={cancel}
              />
            )}
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
                      <SmartIdTab onStart={() => start('smart-id')} />
                    </>
                  ),
                },
                {
                  labelId: 'login.mobile.id',
                  content: (
                    <>
                      {errorAlert}
                      <MobileIdForm
                        onSubmit={(personalCode, phoneNumber) =>
                          start('mobile-id', { personalCode, phoneNumber })
                        }
                      />
                    </>
                  ),
                },
                {
                  labelId: 'login.id.card',
                  hideOnMobile: true,
                  content: (
                    <>
                      {errorAlert}
                      <IdCardTab
                        loading={pending?.method === 'id-card'}
                        onStart={() => start('id-card')}
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
