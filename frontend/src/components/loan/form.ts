import type { ExtractedContract, LoanFieldKey, LoanTerms, RateType, RepaymentType } from './api'

/** The fields the borrower confirms, in the order the form shows them. */
export const confirmableFields = [
  { key: 'LENDER', name: 'lender', kind: 'text', required: true },
  { key: 'CONTRACT_NUMBER', name: 'contractNumber', kind: 'text' },
  { key: 'CONTRACT_DATE', name: 'contractDate', kind: 'date' },
  { key: 'PRINCIPAL', name: 'principal', kind: 'euro' },
  { key: 'MARGIN', name: 'margin', kind: 'percent', required: true },
  { key: 'BASE_RATE', name: 'baseRate', kind: 'text' },
  { key: 'RATE_TYPE', name: 'rateType', kind: 'rateType' },
  { key: 'FIXED_RATE_UNTIL', name: 'fixedRateUntil', kind: 'date' },
  { key: 'MATURITY_DATE', name: 'maturityDate', kind: 'date', required: true },
  { key: 'MONTHLY_PAYMENT', name: 'monthlyPayment', kind: 'euro' },
  { key: 'REPAYMENT_TYPE', name: 'repaymentType', kind: 'repaymentType' },
  { key: 'BALANCE', name: 'balance', kind: 'euro' },
  { key: 'PROPERTY_VALUE', name: 'propertyValue', kind: 'euro' },
] as const satisfies readonly {
  key: LoanFieldKey
  name: keyof FormValues
  kind: string
  required?: boolean
}[]

export type FormValues = {
  lender: string
  contractNumber: string
  contractDate: string
  principal: string
  margin: string
  baseRate: string
  rateType: RateType
  fixedRateUntil: string
  maturityDate: string
  monthlyPayment: string
  repaymentType: RepaymentType
  balance: string
  propertyValue: string
}

type Source = Pick<
  ExtractedContract,
  | 'lender'
  | 'contractNumber'
  | 'contractDate'
  | 'principal'
  | 'margin'
  | 'baseRate'
  | 'rateType'
  | 'fixedRateUntil'
  | 'maturityDate'
  | 'monthlyPayment'
  | 'repaymentType'
  | 'balance'
  | 'propertyValue'
>

const text = (value: string | number | null | undefined) =>
  value === null || value === undefined ? '' : String(value)

export function toFormValues(source: Source | null): FormValues {
  return {
    lender: text(source?.lender),
    contractNumber: text(source?.contractNumber),
    contractDate: text(source?.contractDate),
    principal: text(source?.principal),
    margin: text(source?.margin),
    baseRate: text(source?.baseRate),
    rateType: source?.rateType ?? 'VARIABLE',
    fixedRateUntil: text(source?.fixedRateUntil),
    maturityDate: text(source?.maturityDate),
    monthlyPayment: text(source?.monthlyPayment),
    repaymentType: source?.repaymentType ?? 'ANNUITY',
    balance: text(source?.balance),
    propertyValue: text(source?.propertyValue),
  }
}

/** Accepts "1,95", "185 000" and "185000.00"; empty means unknown. */
export function parseNumber(value: string): number | null {
  const compact = value.replace(/[\s €%]/g, '').replace(',', '.')
  if (compact === '') {
    return null
  }
  const number = Number(compact)
  return Number.isFinite(number) ? number : null
}

export function isNumber(value: string) {
  return value.trim() === '' || parseNumber(value) !== null
}

export function toTerms(values: FormValues, documentId: string | null): LoanTerms {
  const orNull = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    documentId,
    lender: orNull(values.lender),
    contractNumber: orNull(values.contractNumber),
    contractDate: orNull(values.contractDate),
    principal: parseNumber(values.principal),
    margin: parseNumber(values.margin),
    baseRate: orNull(values.baseRate),
    rateType: values.rateType,
    fixedRateUntil: values.rateType === 'VARIABLE' ? null : orNull(values.fixedRateUntil),
    maturityDate: orNull(values.maturityDate),
    monthlyPayment: parseNumber(values.monthlyPayment),
    repaymentType: values.repaymentType,
    balance: parseNumber(values.balance),
    propertyValue: parseNumber(values.propertyValue),
  }
}
