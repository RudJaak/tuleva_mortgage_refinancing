import type { Stage } from '../documents/progress'

export const statementStages: readonly Stage[] = [
  { fromSeconds: 0, id: 'statements.progress.reading' },
  { fromSeconds: 6, id: 'statements.progress.income' },
  { fromSeconds: 13, id: 'statements.progress.obligations' },
  { fromSeconds: 21, id: 'statements.progress.calculating' },
  { fromSeconds: 45, id: 'documents.progress.slow' },
]
