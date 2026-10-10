import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'

export type VerificationLevel =
  'BANK_SEALED' | 'UNTRUSTED_SEAL' | 'INVALID_SEAL' | 'UNSIGNED_CONTAINER' | 'PLAIN_PDF'

export type Verification = {
  level: VerificationLevel
  bankName: string | null
  sealedBy: string | null
  sealedAt: string | null
}

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

export type StatementDocument = {
  documentId: string
  type: 'ACCOUNT_STATEMENT'
  status: 'RECEIVED' | 'PROCESSED' | 'FAILED'
  verification: Verification
  createdAt: string
  result: StatementResult | null
  failure: { reason: string; message: string } | null
}

const documentsPath = '/v1/documents?type=ACCOUNT_STATEMENT'
const documentsKey = ['documents', 'ACCOUNT_STATEMENT']

export const statementPollMs = 1_500

export function useStatementDocuments() {
  return useQuery({
    queryKey: documentsKey,
    queryFn: () => api.get<StatementDocument[]>(documentsPath),
    refetchInterval: (query) =>
      query.state.data?.some((document) => document.status === 'RECEIVED')
        ? statementPollMs
        : false,
  })
}

export function useUploadStatement() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('type', 'ACCOUNT_STATEMENT')
      form.append('file', file)
      return api.upload<StatementDocument>('/v1/documents', form)
    },
    onSuccess: (accepted) => {
      queryClient.setQueryData<StatementDocument[]>(documentsKey, (current = []) => [
        accepted,
        ...current.filter((document) => document.documentId !== accepted.documentId),
      ])
    },
  })
}
