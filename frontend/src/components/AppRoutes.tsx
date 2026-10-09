import { Route, Routes } from 'react-router-dom'
import { DonePage } from './done/DonePage'
import { FlowLayout } from './flow/FlowLayout'
import { FlowStepPage } from './flow/FlowStepPage'
import { LandingPage } from './landing/LandingPage'
import { Layout } from './layout/Layout'
import { NotFoundPage } from './layout/NotFoundPage'
import { LoginPage } from './login/LoginPage'
import { flowSteps, paths } from './paths'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path={paths.landing} element={<LandingPage />} />
        <Route path={paths.login} element={<LoginPage />} />
        <Route element={<FlowLayout />}>
          {flowSteps.map((step) => (
            <Route
              key={step.path}
              path={step.path}
              element={<FlowStepPage section={step.section} />}
            />
          ))}
        </Route>
        <Route path={paths.done} element={<DonePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
