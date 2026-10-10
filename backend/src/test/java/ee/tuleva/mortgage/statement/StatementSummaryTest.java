package ee.tuleva.mortgage.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class StatementSummaryTest {

  @Test
  void aSummaryStoredBeforeNewerFieldsExistedReadsThemAsEmpty() {
    String stored =
        """
        {"periodStart": "2026-04-10", "periodEnd": "2026-10-10", "monthsCovered": 6.0,
         "income": {"averageMonthlySalary": 2400, "lowestMonthlySalary": 2350,
                    "monthsWithSalary": 6, "employers": 1, "averageMonthlyOtherIncome": 80,
                    "averageMonthlyTotal": 2480},
         "obligations": [], "monthlyObligations": 0, "debtServiceRatio": 0,
         "monthlySavings": 100, "lowestBalance": 10, "holderCheck": "MATCH", "flags": []}
        """;

    var summary = JsonMapper.builder().build().readValue(stored, StatementSummary.class);

    assertThat(summary.oneOffPayments()).isEmpty();
    assertThat(summary.monthlyCardRepayments()).isEqualByComparingTo("0");
    assertThat(summary.monthsAnalysed()).isEqualByComparingTo("6.0");
    assertThat(summary.income().oneOffIncome()).isEmpty();
  }
}
