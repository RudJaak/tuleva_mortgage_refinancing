import type { Stage } from '../documents/progress'

export const contractStages: readonly Stage[] = [
  { fromSeconds: 0, id: 'loan.progress.reading' },
  { fromSeconds: 6, id: 'loan.progress.amounts' },
  { fromSeconds: 13, id: 'loan.progress.dates' },
  { fromSeconds: 20, id: 'loan.progress.summary' },
  { fromSeconds: 45, id: 'documents.progress.slow' },
]
