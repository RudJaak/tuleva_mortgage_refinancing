import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

export type Milestone = { volume: number; margin: number }

export type PublicConfig = {
  calculator: {
    euribor6m: number
    euriborDate: string
    defaultMargin: number
    marketAverageMargin: number
    switchingCost: number
    annualDiscountRate: number
  }
  pool: {
    deadline: string
    minimumVolume: number
    milestones: Milestone[]
  }
}

export type MilestoneStatus = Milestone & { reached: boolean }

export type PoolStats = {
  joinedCount: number | null
  committedVolume: number | null
  minimumVolume: number
  minimumReached: boolean
  deadline: string
  milestones: MilestoneStatus[]
  currentMilestone: MilestoneStatus | null
  nextMilestone: MilestoneStatus | null
}

export const poolStatsRefreshMs = 5_000

export function usePublicConfig() {
  return useQuery({
    queryKey: ['config', 'public'],
    queryFn: () => api.get<PublicConfig>('/v1/config/public'),
    staleTime: Infinity,
  })
}

export function usePoolStats() {
  return useQuery({
    queryKey: ['pool', 'stats'],
    queryFn: () => api.get<PoolStats>('/v1/pool/stats'),
    refetchInterval: poolStatsRefreshMs,
  })
}
