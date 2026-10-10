import { useDocuments, useUploadDocument, type UploadedDocument } from '../documents/api'

export type FlagCode =
  | 'NO_SALARY'
  | 'IRREGULAR_SALARY'
  | 'SHORT_PERIOD'
  | 'NEGATIVE_BALANCE'
  | 'LATE_FEES'
  | 'DEBT_COLLECTION'
  | 'ENFORCEMENT'
  | 'GAMBLING'
  | 'HOLDER_MISMATCH'

export type StatementSummary = {
  periodStart: string | null
  periodEnd: string | null
  monthsCovered: number
  income: {
    averageMonthlySalary: number
    lowestMonthlySalary: number
    monthsWithSalary: number
    employers: number
    averageMonthlyOtherIncome: number
    averageMonthlyTotal: number
  }
  obligations: {
    category: string
    creditor: string | null
    typicalMonthly: number
    months: number
  }[]
  monthlyObligations: number
  oneOffPayments: { category: string; creditor: string | null; date: string; amount: number }[]
  monthlyCardRepayments: number
  debtServiceRatio: number | null
  monthlySavings: number
  lowestBalance: number | null
  holderCheck: 'MATCH' | 'MISMATCH' | 'NOT_CHECKED'
  flags: { code: FlagCode; occurrences: number }[]
}

export type StatementResult = {
  account: string | null
  summary: StatementSummary
}

export type StatementDocument = UploadedDocument<StatementResult>

export function useStatementDocuments() {
  return useDocuments<StatementResult>('ACCOUNT_STATEMENT')
}

export function useUploadStatement() {
  return useUploadDocument<StatementResult>('ACCOUNT_STATEMENT')
}
