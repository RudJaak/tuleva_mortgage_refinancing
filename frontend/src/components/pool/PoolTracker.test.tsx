import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { mockApi, renderWithProviders } from '../../test/render'
import { midwayStats } from './fixtures'
import { PoolTracker } from './PoolTracker'

describe('PoolTracker', () => {
  it('shows how many have joined and their combined balance', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(await screen.findByTestId('joined-count')).toHaveTextContent('520')
    expect(screen.getByTestId('committed-volume')).toHaveTextContent('60 mln €')
  })

  it('tracks progress towards the 100 million minimum', async () => {
    mockApi({ '/v1/pool/stats': midwayStats })
    renderWithProviders(<PoolTracker />)

    expect(
      await screen.findByText('Miinimum 100 mln €: eesmärk-marginaal 1,20%'),
    ).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '60')
    expect(screen.getByText('Puudu 40 mln €')).toBeInTheDocument()
  })

  it('lists all three milestones and marks the reached ones', async () => {
    mockApi({
      '/v1/pool/stats': {
        ...midwayStats,
        committedVolume: 120_000_000,
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
      screen.getByText('Järgmine verstapost 300 mln €: eesmärk-marginaal 1,00%'),
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
