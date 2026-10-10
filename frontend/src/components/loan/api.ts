import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'
import { useDocuments, useUploadDocument, type UploadedDocument } from '../documents/api'

export type LoanFieldKey =
  | 'LENDER'
  | 'CONTRACT_NUMBER'
  | 'CONTRACT_DATE'
  | 'BORROWERS'
  | 'PRINCIPAL'
  | 'CURRENCY'
  | 'BASE_RATE'
  | 'MARGIN'
  | 'RATE_TYPE'
  | 'FIXED_RATE_UNTIL'
  | 'STATED_INTEREST_RATE'
  | 'MATURITY_DATE'
  | 'TERM_MONTHS'
  | 'MONTHLY_PAYMENT'
  | 'PAYMENT_DAY'
  | 'REPAYMENT_TYPE'
  | 'MORTGAGE_AMOUNT'
  | 'COLLATERAL'
  | 'EARLY_REPAYMENT_TERMS'
  | 'PROPERTY_VALUE'
  | 'BALANCE'

export type Evidence = {
  document: string
  page: number
  quote: string
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
}

export type RateType = 'VARIABLE' | 'FIXED' | 'MIXED' | 'UNKNOWN'
export type RepaymentType = 'ANNUITY' | 'EQUAL_PRINCIPAL' | 'UNKNOWN'

export type ExtractedContract = {
  lender: string | null
  contractNumber: string | null
  contractDate: string | null
  borrowers: string[]
  principal: number | null
  currency: string | null
  baseRate: string | null
  margin: number | null
  rateType: RateType
  fixedRateUntil: string | null
  statedInterestRate: number | null
  maturityDate: string | null
  termMonths: number | null
  monthlyPayment: number | null
  paymentDay: number | null
  repaymentType: RepaymentType
  mortgageAmount: number | null
  collateral: string | null
  earlyRepaymentTerms: string | null
  propertyValue: number | null
  balance: number | null
  balanceDate: string | null
  evidence: Partial<Record<LoanFieldKey, Evidence>>
}

export type LoanFlag =
  | 'FIXED_RATE_PERIOD'
  | 'MARGIN_AT_OR_BELOW_TARGET'
  | 'REMAINING_TERM_UNDER_MINIMUM'
  | 'BALANCE_UNDER_MINIMUM'
  | 'CO_BORROWERS'
  | 'BORROWER_NOT_A_PARTY'
  | 'NOT_SIGNED_BY_BORROWER'
  | 'MISSING_KEY_FIELDS'

export type LoanSummary = {
  currentInterestRate: number | null
  rateBasis: 'EURIBOR_PLUS_MARGIN' | 'FIXED' | 'UNKNOWN'
  euribor6m: number
  euriborDate: string
  maturityDate: string | null
  remainingMonths: number | null
  balance: number | null
  balanceSource: 'STATED' | 'ESTIMATED' | 'UNKNOWN'
  loanToValue: number | null
  ageAtMaturity: number | null
  borrowerCount: number
  partyCheck: 'MATCH' | 'MISMATCH' | 'NOT_CHECKED'
  missingFields: LoanFieldKey[]
  lowConfidenceFields: LoanFieldKey[]
  flags: LoanFlag[]
}

export type LoanContractResult = { contract: ExtractedContract; summary: LoanSummary }
export type LoanContractDocument = UploadedDocument<LoanContractResult>

export type LoanTerms = {
  documentId: string | null
  lender: string | null
  contractNumber: string | null
  contractDate: string | null
  principal: number | null
  margin: number | null
  baseRate: string | null
  rateType: RateType
  fixedRateUntil: string | null
  maturityDate: string | null
  monthlyPayment: number | null
  repaymentType: RepaymentType
  balance: number | null
  propertyValue: number | null
}

export type LoanView = {
  terms: LoanTerms
  provenance: Partial<Record<LoanFieldKey, 'DOCUMENT' | 'DECLARED'>>
  evidence: Partial<Record<LoanFieldKey, Evidence>>
  summary: LoanSummary
  confirmedAt: string
}

const loanKey = ['me', 'loan']

/** The borrower's confirmed loan, or null before they have confirmed one. */
export function useLoan() {
  return useQuery({
    queryKey: loanKey,
    queryFn: () =>
      api.get<LoanView>('/v1/me/loan').catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 404) {
          return null
        }
        throw error
      }),
  })
}

export function useConfirmLoan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (terms: LoanTerms) => api.put<LoanView>('/v1/me/loan', terms),
    onSuccess: (view) => queryClient.setQueryData(loanKey, view),
  })
}

export function useLoanContracts() {
  return useDocuments<LoanContractResult>('LOAN_CONTRACT')
}

export function useUploadLoanContract() {
  return useUploadDocument<LoanContractResult>('LOAN_CONTRACT')
}
