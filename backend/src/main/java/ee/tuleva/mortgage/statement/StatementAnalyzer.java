package ee.tuleva.mortgage.statement;

import static java.math.RoundingMode.HALF_UP;

import ee.tuleva.mortgage.borrower.PersonNames;
import ee.tuleva.mortgage.statement.ExtractedStatement.Transaction;
import ee.tuleva.mortgage.statement.StatementSummary.Flag;
import ee.tuleva.mortgage.statement.StatementSummary.FlagCode;
import ee.tuleva.mortgage.statement.StatementSummary.HolderCheck;
import ee.tuleva.mortgage.statement.StatementSummary.Income;
import ee.tuleva.mortgage.statement.StatementSummary.Obligation;
import ee.tuleva.mortgage.statement.StatementSummary.OneOffIncome;
import ee.tuleva.mortgage.statement.StatementSummary.OneOffPayment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Turns an extracted statement into screening figures. The extractor only categorises; all
 * arithmetic happens here, so it is deterministic and tested.
 */
@Component
@RequiredArgsConstructor
class StatementAnalyzer {

  /** Banks look at the last six months; a longer statement is analysed over its last six. */
  static final int ANALYSIS_MONTHS = 6;

  /** We ask for six months; anything clearly shorter is flagged. */
  static final BigDecimal MINIMUM_MONTHS = new BigDecimal("5.5");

  /** A statement ending longer ago than this no longer shows the borrower's current situation. */
  static final int MAX_AGE_DAYS = 45;

  /** Other income counts as regular only when the same payer pays in this many months. */
  static final int REGULAR_INCOME_MONTHS = 3;

  /** A salary month this far below the average counts as irregular. */
  static final BigDecimal IRREGULAR_SALARY_SHARE = new BigDecimal("0.7");

  /** A payment this many times the creditor's median instalment is a one-off, not a month. */
  static final BigDecimal ONE_OFF_FACTOR = new BigDecimal("3");

  private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30.4375");

  private record Obligations(List<Obligation> recurring, List<OneOffPayment> oneOffs) {}

  private record Window(@Nullable LocalDate from, @Nullable LocalDate to, BigDecimal months) {}

  private final Clock clock;
  private final StatementProperties properties;

  private static final Map<TransactionCategory, FlagCode> WARNING_FLAGS =
      Map.of(
          TransactionCategory.LATE_FEE, FlagCode.LATE_FEES,
          TransactionCategory.DEBT_COLLECTION, FlagCode.DEBT_COLLECTION,
          TransactionCategory.ENFORCEMENT, FlagCode.ENFORCEMENT,
          TransactionCategory.GAMBLING, FlagCode.GAMBLING);

  StatementSummary analyze(
      ExtractedStatement statement, @Nullable String firstName, @Nullable String lastName) {
    BigDecimal monthsCovered =
        monthsBetween(statement.periodStart(), statement.periodEnd(), statement.transactions());
    Window window =
        properties.recencyChecks()
            ? window(statement, monthsCovered)
            : new Window(statement.periodStart(), statement.periodEnd(), monthsCovered);
    var transactions =
        statement.transactions().stream()
            .filter(t -> window.from() == null || !t.date().isBefore(window.from()))
            .filter(t -> window.to() == null || !t.date().isAfter(window.to()))
            .toList();
    BigDecimal months = window.months().setScale(0, HALF_UP).max(BigDecimal.ONE);

    Income income = income(transactions, months);
    Obligations obligations = obligations(transactions, months);
    BigDecimal monthlyObligations =
        obligations.recurring().stream()
            .map(Obligation::typicalMonthly)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal monthlyCardRepayments =
        perMonth(total(of(transactions, TransactionCategory.CREDIT_CARD)), months);
    BigDecimal debtServiceRatio =
        income.averageMonthlyTotal().signum() > 0
            ? monthlyObligations.divide(income.averageMonthlyTotal(), 2, HALF_UP)
            : null;
    BigDecimal monthlySavings =
        perMonth(total(of(transactions, TransactionCategory.SAVINGS)), months);
    HolderCheck holderCheck = holderCheck(statement.accountHolder(), firstName, lastName);

    List<Flag> flags = new ArrayList<>();
    if (income.monthsWithSalary() == 0) {
      flags.add(new Flag(FlagCode.NO_SALARY, 1));
    } else if (income.monthsWithSalary() < months.intValue() - 1
        || income
                .lowestMonthlySalary()
                .compareTo(income.averageMonthlySalary().multiply(IRREGULAR_SALARY_SHARE))
            < 0) {
      flags.add(new Flag(FlagCode.IRREGULAR_SALARY, 1));
    }
    if (monthsCovered.compareTo(MINIMUM_MONTHS) < 0) {
      flags.add(new Flag(FlagCode.SHORT_PERIOD, 1));
    }
    if (properties.recencyChecks()
        && statement.periodEnd() != null
        && statement.periodEnd().isBefore(LocalDate.now(clock).minusDays(MAX_AGE_DAYS))) {
      flags.add(new Flag(FlagCode.STALE_STATEMENT, 1));
    }
    if (statement.lowestBalance() != null && statement.lowestBalance().signum() < 0) {
      flags.add(new Flag(FlagCode.NEGATIVE_BALANCE, 1));
    }
    var warnings = new EnumMap<FlagCode, Integer>(FlagCode.class);
    transactions.stream()
        .filter(t -> WARNING_FLAGS.containsKey(t.category()))
        .forEach(t -> warnings.merge(WARNING_FLAGS.get(t.category()), 1, Integer::sum));
    warnings.forEach((code, count) -> flags.add(new Flag(code, count)));
    if (holderCheck == HolderCheck.MISMATCH) {
      flags.add(new Flag(FlagCode.HOLDER_MISMATCH, 1));
    }

    return new StatementSummary(
        statement.periodStart(),
        statement.periodEnd(),
        monthsCovered,
        window.from(),
        window.to(),
        window.months(),
        income,
        obligations.recurring(),
        monthlyObligations,
        obligations.oneOffs(),
        monthlyCardRepayments,
        debtServiceRatio,
        monthlySavings,
        statement.lowestBalance(),
        holderCheck,
        flags);
  }

  private static BigDecimal monthsBetween(
      @Nullable LocalDate from, @Nullable LocalDate to, List<Transaction> transactions) {
    if (from != null && to != null) {
      long days = ChronoUnit.DAYS.between(from, to) + 1;
      return BigDecimal.valueOf(days).divide(DAYS_PER_MONTH, 1, HALF_UP);
    }
    long distinct = transactions.stream().map(t -> YearMonth.from(t.date())).distinct().count();
    return BigDecimal.valueOf(distinct).setScale(1, HALF_UP);
  }

  /**
   * The last {@value #ANALYSIS_MONTHS} full calendar months of a longer statement, so that payments
   * made on the 1st are not cut off; a statement of about six months is analysed whole.
   */
  private static Window window(ExtractedStatement statement, BigDecimal monthsCovered) {
    LocalDate start = statement.periodStart();
    LocalDate end = statement.periodEnd();
    if (start == null
        || end == null
        || monthsCovered.compareTo(BigDecimal.valueOf(ANALYSIS_MONTHS).add(new BigDecimal("0.5")))
            <= 0) {
      return new Window(start, end, monthsCovered);
    }
    YearMonth lastFull =
        end.equals(YearMonth.from(end).atEndOfMonth())
            ? YearMonth.from(end)
            : YearMonth.from(end).minusMonths(1);
    LocalDate from = lastFull.minusMonths(ANALYSIS_MONTHS - 1).atDay(1);
    LocalDate to = lastFull.atEndOfMonth();
    return new Window(from, to, BigDecimal.valueOf(ANALYSIS_MONTHS).setScale(1, HALF_UP));
  }

  private static Income income(List<Transaction> transactions, BigDecimal months) {
    var salaries = of(transactions, TransactionCategory.SALARY);
    Map<YearMonth, BigDecimal> salaryByMonth =
        salaries.stream()
            .collect(
                Collectors.groupingBy(
                    t -> YearMonth.from(t.date()),
                    Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
    BigDecimal averageSalary = perMonth(total(salaries), months);
    BigDecimal lowestSalary =
        salaryByMonth.values().stream()
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO)
            .setScale(2, HALF_UP);
    int employers =
        (int) salaries.stream().map(t -> normalise(t.counterparty())).distinct().count();
    List<Transaction> regularOther = new ArrayList<>();
    List<OneOffIncome> oneOffIncome = new ArrayList<>();
    of(transactions, TransactionCategory.OTHER_INCOME).stream()
        .collect(
            Collectors.groupingBy(
                t -> normalise(t.counterparty()), LinkedHashMap::new, Collectors.toList()))
        .values()
        .forEach(
            payments -> {
              long paidMonths =
                  payments.stream().map(t -> YearMonth.from(t.date())).distinct().count();
              if (paidMonths >= REGULAR_INCOME_MONTHS) {
                regularOther.addAll(payments);
              } else {
                payments.forEach(
                    t ->
                        oneOffIncome.add(
                            new OneOffIncome(t.counterparty().strip(), t.date(), t.amount())));
              }
            });
    oneOffIncome.sort(Comparator.comparing(OneOffIncome::date));
    BigDecimal averageOther = perMonth(total(regularOther), months);
    return new Income(
        averageSalary,
        lowestSalary,
        salaryByMonth.size(),
        employers,
        averageOther,
        averageSalary.add(averageOther),
        List.copyOf(oneOffIncome));
  }

  private static Obligations obligations(List<Transaction> transactions, BigDecimal months) {
    Map<String, List<Transaction>> byCreditor =
        transactions.stream()
            .filter(t -> t.category().kind() == TransactionCategory.Kind.OBLIGATION)
            .collect(
                Collectors.groupingBy(
                    t -> t.category() + "|" + normalise(t.counterparty()),
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<Obligation> recurring = new ArrayList<>();
    List<OneOffPayment> oneOffs = new ArrayList<>();
    for (List<Transaction> group : byCreditor.values()) {
      Transaction first = group.getFirst();
      String creditor =
          TransactionCategory.PRIVATE_PAYEE.contains(first.category())
              ? null
              : first.counterparty().strip();
      List<Transaction> regular = group;
      if (group.size() >= 3) {
        BigDecimal limit =
            median(group.stream().map(t -> t.amount().abs()).toList()).multiply(ONE_OFF_FACTOR);
        regular = group.stream().filter(t -> t.amount().abs().compareTo(limit) <= 0).toList();
        group.stream()
            .filter(t -> t.amount().abs().compareTo(limit) > 0)
            .forEach(
                t ->
                    oneOffs.add(
                        new OneOffPayment(t.category(), creditor, t.date(), t.amount().abs())));
      }
      Collection<BigDecimal> monthlyTotals =
          regular.stream()
              .collect(
                  Collectors.groupingBy(
                      t -> YearMonth.from(t.date()),
                      Collectors.reducing(BigDecimal.ZERO, t -> t.amount().abs(), BigDecimal::add)))
              .values();
      // Too few payments to call one typical: spread them over the statement instead
      BigDecimal typical =
          monthlyTotals.size() >= 3
              ? median(monthlyTotals).setScale(2, HALF_UP)
              : perMonth(total(regular), months);
      recurring.add(new Obligation(first.category(), creditor, typical, monthlyTotals.size()));
    }
    recurring.sort(Comparator.comparing(Obligation::typicalMonthly).reversed());
    oneOffs.sort(Comparator.comparing(OneOffPayment::date));
    return new Obligations(List.copyOf(recurring), List.copyOf(oneOffs));
  }

  private static BigDecimal median(Collection<BigDecimal> values) {
    List<BigDecimal> sorted = values.stream().sorted().toList();
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 1
        ? sorted.get(middle)
        : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.TWO, 2, HALF_UP);
  }

  static HolderCheck holderCheck(
      @Nullable String accountHolder, @Nullable String firstName, @Nullable String lastName) {
    return HolderCheck.valueOf(PersonNames.match(accountHolder, firstName, lastName).name());
  }

  private static String normalise(String text) {
    return PersonNames.normalise(text);
  }

  private static List<Transaction> of(
      List<Transaction> transactions, TransactionCategory category) {
    return transactions.stream().filter(t -> t.category() == category).toList();
  }

  private static BigDecimal total(Collection<Transaction> transactions) {
    return transactions.stream()
        .map(Transaction::amount)
        .map(BigDecimal::abs)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal perMonth(BigDecimal total, BigDecimal months) {
    return total.divide(months, 2, HALF_UP);
  }
}
