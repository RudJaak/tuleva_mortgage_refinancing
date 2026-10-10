import type { StatementDocument } from './api'

export const sealed = {
  level: 'BANK_SEALED',
  bankName: 'LHV',
  sealedBy: 'AS LHV Pank: Digital Stamping of Document V2',
  sealedAt: '2026-10-10T06:23:55Z',
} as const

export const inProgress: StatementDocument = {
  documentId: 'd1',
  type: 'ACCOUNT_STATEMENT',
  status: 'RECEIVED',
  verification: sealed,
  createdAt: new Date().toISOString(),
  result: null,
  failure: null,
}

export const processed: StatementDocument = {
  ...inProgress,
  status: 'PROCESSED',
  result: {
    account: 'EE…3456',
    summary: {
      periodStart: '2026-04-10',
      periodEnd: '2026-10-10',
      monthsCovered: 6,
      income: {
        averageMonthlySalary: 2400,
        lowestMonthlySalary: 2350,
        monthsWithSalary: 6,
        employers: 1,
        averageMonthlyOtherIncome: 80,
        averageMonthlyTotal: 2480,
      },
      obligations: [
        { category: 'MORTGAGE_PAYMENT', creditor: 'LHV Pank', typicalMonthly: 712.4, months: 6 },
        { category: 'LEASING', creditor: 'Autoliising AS', typicalMonthly: 289, months: 6 },
      ],
      monthlyObligations: 1001.4,
      oneOffPayments: [
        { category: 'LEASING', creditor: 'Autoliising AS', date: '2026-04-15', amount: 3500 },
      ],
      monthlyCardRepayments: 1200,
      debtServiceRatio: 0.4,
      monthlySavings: 100,
      lowestBalance: 312.55,
      holderCheck: 'MATCH',
      flags: [{ code: 'LATE_FEES', occurrences: 1 }],
    },
  },
}
