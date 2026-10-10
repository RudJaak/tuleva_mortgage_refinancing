export const paths = {
  landing: '/',
  login: '/sisene',
  loan: '/laen',
  property: '/vara',
  statements: '/valjavotted',
  review: '/kinnitus',
  done: '/valmis',
} as const

export const flowSteps = [
  { path: paths.loan, labelId: 'flow.step.loan', section: 'loan' },
  { path: paths.property, labelId: 'flow.step.property', section: 'property' },
  { path: paths.statements, labelId: 'flow.step.statements', section: 'statements' },
  { path: paths.review, labelId: 'flow.step.review', section: 'review' },
] as const

export type FlowStep = (typeof flowSteps)[number]
