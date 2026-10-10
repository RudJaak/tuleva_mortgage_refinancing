import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { mockApi, renderWithProviders } from '../../test/render'
import { midwayStats } from './fixtures'
import { PoolTracker } from './PoolTracker'

describe('PoolTracker', () => {
  it('counts people, not money', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByTestId('joined-count')).toHaveTextContent('520')
    expect(screen.queryByText(/€/)).not.toBeInTheDocument()
    expect(screen.getByText(/laenujäägi suurus ei ole oluline/)).toBeInTheDocument()
  })

  it('tracks progress towards the minimum number of people', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(
      await screen.findByText('Miinimum 1000 inimest: eesmärk-marginaal 1,35%'),
    ).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '52')
    expect(screen.getByText('Puudu 480 inimest')).toBeInTheDocument()
  })

  it('lists all three milestones and marks the reached ones', async () => {
    mockApi({
      '/v1/pool/stats': {
        ...midwayStats,
        joinedCount: 1200,
        minimumReached: true,
        milestones: midwayStats.milestones.map((m, i) => ({ ...m, reached: i === 0 })),
        currentMilestone: { ...midwayStats.milestones[0], reached: true },
        nextMilestone: midwayStats.milestones[1],
      },
    })
    renderWithProviders(<PoolTracker />)

    const cards = await screen.findAllByTestId('milestone')
    expect(cards).toHaveLength(3)
    expect(within(cards[0]).getByText('✓ Saavutatud')).toBeInTheDocument()
    expect(within(cards[1]).queryByText('✓ Saavutatud')).not.toBeInTheDocument()
    expect(
      screen.getByText('Järgmine verstapost 2500 inimest: eesmärk-marginaal 1,30%'),
    ).toBeInTheDocument()
  })

  it('hides the numbers while the pool is too small to stay anonymous', async () => {
    mockApi({ '/v1/pool/stats': { ...midwayStats, joinedCount: null, committedVolume: null } })
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByText(/vähemalt kümme/)).toBeInTheDocument()
    expect(screen.getByTestId('joined-count')).toHaveTextContent('—')
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
  })

  it('counts down to the deadline', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByRole('timer')).toHaveAccessibleName(/^Liitumise lõpuni \d+ päeva/)
  })

  it('says so when the stats cannot be loaded', async () => {
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByText(/ei õnnestunud praegu laadida/)).toBeInTheDocument()
  })

  it('presents margins as goals, not as an offer', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByText(/mitte pakkumine/)).toBeInTheDocument()
  })
})
