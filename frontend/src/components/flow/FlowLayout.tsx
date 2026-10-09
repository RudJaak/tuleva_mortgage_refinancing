import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { flowSteps, paths } from '../paths'
import { Stepper } from './Stepper'

export function FlowLayout() {
  const { pathname } = useLocation()
  const currentIndex = flowSteps.findIndex((step) => step.path === pathname)
  if (currentIndex < 0) {
    return <Navigate to={paths.loan} replace />
  }
  return (
    <div className="d-flex flex-column gap-5">
      <Stepper currentIndex={currentIndex} />
      <Outlet />
    </div>
  )
}
