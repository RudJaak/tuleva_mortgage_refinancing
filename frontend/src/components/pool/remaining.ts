export type Remaining = { days: number; hours: number; minutes: number; seconds: number }

export function remainingUntil(deadline: Date, now: Date): Remaining {
  const totalSeconds = Math.max(0, Math.floor((deadline.getTime() - now.getTime()) / 1000))
  return {
    days: Math.floor(totalSeconds / 86_400),
    hours: Math.floor((totalSeconds % 86_400) / 3600),
    minutes: Math.floor((totalSeconds % 3600) / 60),
    seconds: totalSeconds % 60,
  }
}
