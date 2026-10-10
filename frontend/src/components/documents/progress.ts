/** Reading usually takes about half a minute; the bar approaches but never reaches 100%. */
export const typicalSeconds = 30

export function estimatedProgress(elapsedMs: number, seconds = typicalSeconds): number {
  const elapsed = Math.max(0, elapsedMs) / 1000
  return Math.min(95, Math.round(100 * (1 - Math.exp(-elapsed / (seconds / 2)))))
}

export type Stage = { fromSeconds: number; id: string }

export function stageFor(elapsedMs: number, stages: readonly Stage[]): string {
  const seconds = elapsedMs / 1000
  return stages.filter((stage) => seconds >= stage.fromSeconds).at(-1)?.id ?? stages[0].id
}
