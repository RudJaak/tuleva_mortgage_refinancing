import type { LoanContractDocument, LoanSummary, LoanView } from './api'

export const summary: LoanSummary = {
  currentInterestRate: 5.886,
  rateBasis: 'EURIBOR_PLUS_MARGIN',
  euribor6m: 2.936,
  euriborDate: '2026-10-08',
  maturityDate: '2045-03-05',
  remainingMonths: 220,
  balance: 66744,
  balanceSource: 'ESTIMATED',
  loanToValue: 0.41,
  ageAtMaturity: 64,
  borrowerCount: 1,
  partyCheck: 'MATCH',
  missingFields: [],
  lowConfidenceFields: ['PROPERTY_VALUE'],
  flags: [],
}

export const processedContract: LoanContractDocument = {
  documentId: 'c1',
  type: 'LOAN_CONTRACT',
  status: 'PROCESSED',
  verification: {
    level: 'SIGNED',
    bankName: null,
    sealedBy: null,
    sealedAt: '2020-03-05T10:00:00Z',
    signers: [
      { name: 'Kati Kask', kind: 'PERSON' },
      { name: 'Mari Maasikas', kind: 'PERSON' },
    ],
    signedByBorrower: true,
  },
  createdAt: new Date().toISOString(),
  failure: null,
  result: {
    summary,
    contract: {
      lender: 'AS LHV Pank',
      contractNumber: 'EAL-2020-1',
      contractDate: '2020-03-05',
      borrowers: ['Mari Maasikas'],
      principal: 82990,
      currency: 'EUR',
      baseRate: '6 kuu Euribor',
      margin: 2.95,
      rateType: 'VARIABLE',
      fixedRateUntil: null,
      statedInterestRate: null,
      maturityDate: '2045-03-05',
      termMonths: 300,
      monthlyPayment: 393.14,
      paymentDay: 5,
      repaymentType: 'ANNUITY',
      mortgageAmount: 107887,
      collateral: 'Tallinn',
      earlyRepaymentTerms: 'Tasu ei rakendata',
      propertyValue: 162000,
      balance: null,
      balanceDate: null,
      evidence: {
        MARGIN: {
          document: 'Laenuleping.pdf',
          page: 1,
          quote: 'Euribor + 2,95% aastas',
          confidence: 'HIGH',
        },
        PROPERTY_VALUE: {
          document: 'Teabeleht.pdf',
          page: 2,
          quote: 'eeldatav väärtus: 162 000 eurot',
          confidence: 'LOW',
        },
      },
    },
  },
}

export const inProgressContract: LoanContractDocument = {
  ...processedContract,
  status: 'RECEIVED',
  result: null,
}

export const confirmedLoan: LoanView = {
  terms: {
    documentId: 'c1',
    lender: 'AS LHV Pank',
    contractNumber: 'EAL-2020-1',
    contractDate: '2020-03-05',
    principal: 82990,
    margin: 2.95,
    baseRate: '6 kuu Euribor',
    rateType: 'VARIABLE',
    fixedRateUntil: null,
    maturityDate: '2045-03-05',
    monthlyPayment: 393.14,
    repaymentType: 'ANNUITY',
    balance: 66000,
    propertyValue: 162000,
  },
  provenance: { LENDER: 'DOCUMENT', MARGIN: 'DOCUMENT', BALANCE: 'DECLARED' },
  evidence: processedContract.result!.contract.evidence,
  summary: { ...summary, balance: 66000, balanceSource: 'STATED' },
  confirmedAt: '2026-10-10T12:00:00Z',
}
