export type Remaining = { days: number; hours: number; minutes: number }

export function remainingUntil(deadline: Date, now: Date): Remaining {
  const totalMinutes = Math.max(0, Math.floor((deadline.getTime() - now.getTime()) / 60_000))
  return {
    days: Math.floor(totalMinutes / (24 * 60)),
    hours: Math.floor((totalMinutes % (24 * 60)) / 60),
    minutes: totalMinutes % 60,
  }
}
