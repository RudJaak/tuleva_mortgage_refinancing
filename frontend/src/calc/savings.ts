export function annuityPayment(
  principal: number,
  annualRatePercent: number,
  months: number,
): number {
  if (principal <= 0 || months <= 0) {
    return 0
  }
  const monthlyRate = annualRatePercent / 100 / 12
  if (monthlyRate === 0) {
    return principal / months
  }
  return (principal * monthlyRate) / (1 - (1 + monthlyRate) ** -months)
}

export function totalInterest(
  principal: number,
  annualRatePercent: number,
  months: number,
): number {
  return annuityPayment(principal, annualRatePercent, months) * months - Math.max(principal, 0)
}

export type MarginComparisonInput = {
  balance: number
  remainingMonths: number
  euribor: number
  currentMargin: number
  targetMargin: number
  switchingCost: number
  annualDiscountRate: number
}

export type MarginComparison = {
  currentPayment: number
  targetPayment: number
  monthlySaving: number
  lifetimeSaving: number
  netPresentValue: number
  breakEvenMonths: number | null
  hasSaving: boolean
}

function presentValueOfMonthlyAmount(amount: number, annualRatePercent: number, months: number) {
  const monthlyRate = annualRatePercent / 100 / 12
  if (monthlyRate === 0) {
    return amount * months
  }
  return (amount * (1 - (1 + monthlyRate) ** -months)) / monthlyRate
}

export function compareMargins(input: MarginComparisonInput): MarginComparison {
  const { balance, remainingMonths, euribor, currentMargin, targetMargin } = input
  const currentPayment = annuityPayment(balance, euribor + currentMargin, remainingMonths)
  const targetPayment = annuityPayment(balance, euribor + targetMargin, remainingMonths)
  const monthlySaving = Math.max(currentPayment - targetPayment, 0)
  if (monthlySaving === 0) {
    return {
      currentPayment,
      targetPayment,
      monthlySaving: 0,
      lifetimeSaving: 0,
      netPresentValue: 0,
      breakEvenMonths: null,
      hasSaving: false,
    }
  }
  const netPresentValue =
    presentValueOfMonthlyAmount(monthlySaving, input.annualDiscountRate, remainingMonths) -
    input.switchingCost
  return {
    currentPayment,
    targetPayment,
    monthlySaving,
    lifetimeSaving: monthlySaving * remainingMonths,
    netPresentValue,
    breakEvenMonths: input.switchingCost / monthlySaving,
    hasSaving: true,
  }
}
