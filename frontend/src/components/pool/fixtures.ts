import type { PoolStats, PublicConfig } from './api'

export const milestones = [
  { members: 1000, margin: 1.3 },
  { members: 2500, margin: 1.25 },
  { members: 5000, margin: 1.2 },
]

export const publicConfig: PublicConfig = {
  calculator: {
    euribor6m: 2.936,
    euriborDate: '2026-10-08',
    defaultMargin: 1.8,
    marketAverageMargin: 1.6,
    switchingCost: 400,
    annualDiscountRate: 3,
    membershipFee: 150,
  },
  pool: { deadline: '2026-11-10T00:00:00+02:00', minimumMembers: 1000, milestones },
}

export const midwayStats: PoolStats = {
  joinedCount: 520,
  committedVolume: 60_000_000,
  minimumMembers: 1000,
  minimumReached: false,
  deadline: '2026-11-10T00:00:00+02:00',
  milestones: milestones.map((m) => ({ ...m, reached: false })),
  currentMilestone: null,
  nextMilestone: { ...milestones[0], reached: false },
}
