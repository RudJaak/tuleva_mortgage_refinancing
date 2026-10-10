import { useMutation, useQueryClient } from '@tanstack/react-query'
import { FormattedMessage } from 'react-intl'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { flowSteps, paths } from '../paths'
import { Stepper } from './Stepper'

export function FlowLayout() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  // Demo only: clears the borrower's documents and loan so the flow can be shown again
  const startOver = useMutation({
    mutationFn: () => api.post<void>('/v1/demo/flow/reset', {}),
    onSettled: async () => {
      await queryClient.resetQueries()
      navigate(paths.loan)
    },
  })
  const currentIndex = flowSteps.findIndex((step) => step.path === pathname)
  if (currentIndex < 0) {
    return <Navigate to={paths.loan} replace />
  }
  return (
    <div className="d-flex flex-column gap-5">
      <div className="d-flex flex-column gap-2">
        <div className="d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-link btn-sm px-0"
            disabled={startOver.isPending}
            onClick={() => startOver.mutate()}
          >
            <FormattedMessage id="flow.reset" />
          </button>
        </div>
        <Stepper currentIndex={currentIndex} />
      </div>
      <Outlet />
    </div>
  )
}
