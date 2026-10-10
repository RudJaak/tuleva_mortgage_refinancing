/** Extraction usually takes about half a minute; the bar approaches but never reaches 100%. */
export const typicalSeconds = 30

export function estimatedProgress(elapsedMs: number): number {
  const seconds = Math.max(0, elapsedMs) / 1000
  return Math.min(95, Math.round(100 * (1 - Math.exp(-seconds / (typicalSeconds / 2)))))
}

const stages = [
  { fromSeconds: 0, id: 'statements.progress.reading' },
  { fromSeconds: 6, id: 'statements.progress.income' },
  { fromSeconds: 13, id: 'statements.progress.obligations' },
  { fromSeconds: 21, id: 'statements.progress.calculating' },
  { fromSeconds: 45, id: 'statements.progress.slow' },
] as const

export type StageId = (typeof stages)[number]['id']

export function stageFor(elapsedMs: number): StageId {
  const seconds = elapsedMs / 1000
  return stages.filter((stage) => seconds >= stage.fromSeconds).at(-1)?.id ?? stages[0].id
}
