import type { PoolStats, PublicConfig } from './api'

export const milestones = [
  { volume: 100_000_000, margin: 1.2 },
  { volume: 300_000_000, margin: 1.0 },
  { volume: 500_000_000, margin: 0.8 },
]

export const publicConfig: PublicConfig = {
  calculator: {
    euribor6m: 2.936,
    euriborDate: '2026-10-08',
    defaultMargin: 1.7,
    marketAverageMargin: 1.6,
    switchingCost: 400,
    annualDiscountRate: 3,
  },
  pool: { deadline: '2026-12-31T23:59:59+02:00', minimumVolume: 100_000_000, milestones },
}

export const midwayStats: PoolStats = {
  joinedCount: 520,
  committedVolume: 60_000_000,
  minimumVolume: 100_000_000,
  minimumReached: false,
  deadline: '2026-12-31T23:59:59+02:00',
  milestones: milestones.map((m) => ({ ...m, reached: false })),
  currentMilestone: null,
  nextMilestone: { ...milestones[0], reached: false },
}
