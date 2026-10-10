package ee.tuleva.mortgage.statement;

import static java.math.RoundingMode.HALF_UP;

import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.borrower.PersonNames;
import ee.tuleva.mortgage.loan.LoanParties;
import ee.tuleva.mortgage.statement.StatementSummary.Flag;
import ee.tuleva.mortgage.statement.StatementSummary.FlagCode;
import ee.tuleva.mortgage.statement.StatementsOverview.Person;
import ee.tuleva.mortgage.statement.StatementsOverview.Totals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Groups the borrower's statements by account holder and adds them up per person and for the
 * household. The expected people are the borrowers named on the contract, or the logged-in borrower
 * without one. An account uploaded more than once counts once, from its newest statement.
 */
@Service
@RequiredArgsConstructor
class StatementsOverviewService {

  static final String UNKNOWN_HOLDER = "?";

  private final StoredStatementSummaryRepository summaries;
  private final LoanParties loanParties;
  private final BorrowerService borrowers;
  private final JsonMapper jsonMapper;

  @Transactional(readOnly = true)
  StatementsOverview overview(UUID borrowerId) {
    List<String> expected = new ArrayList<>(loanParties.borrowerNames(borrowerId));
    if (expected.isEmpty()) {
      borrowers
          .find(borrowerId)
          .map(b -> (b.getFirstName() + " " + b.getLastName()).strip())
          .filter(name -> !name.isBlank())
          .ifPresent(expected::add);
    }

    Map<String, List<StoredStatementSummary>> byPerson = new LinkedHashMap<>();
    expected.forEach(name -> byPerson.put(name, new ArrayList<>()));
    Set<String> countedAccounts = new HashSet<>();
    for (var stored : summaries.findByBorrowerIdOrderByCreatedAtDesc(borrowerId)) {
      String person =
          expected.stream()
              .filter(name -> PersonNames.sameName(name, stored.getAccountHolder()))
              .findFirst()
              .orElse(holderOrDefault(stored.getAccountHolder(), expected));
      String account =
          stored.getAccount() == null
              ? stored.getDocumentId().toString()
              : person + stored.getAccount();
      if (countedAccounts.add(PersonNames.normalise(account))) {
        byPerson.computeIfAbsent(person, k -> new ArrayList<>()).add(stored);
      }
    }

    List<Person> persons = new ArrayList<>();
    List<StatementSummary> all = new ArrayList<>();
    byPerson.forEach(
        (name, rows) -> {
          var parsed = rows.stream().map(this::summary).toList();
          all.addAll(parsed);
          Set<FlagCode> flags = EnumSet.noneOf(FlagCode.class);
          parsed.forEach(s -> s.flags().stream().map(Flag::code).forEach(flags::add));
          persons.add(
              new Person(
                  name,
                  expected.contains(name),
                  rows.stream().map(StoredStatementSummary::getDocumentId).toList(),
                  totals(parsed),
                  flags));
        });
    return new StatementsOverview(persons, totals(all));
  }

  /** A statement without a readable holder belongs to the only expected person, if there is one. */
  private static String holderOrDefault(String holder, List<String> expected) {
    if (holder != null && !holder.isBlank()) {
      return holder;
    }
    return expected.size() == 1 ? expected.getFirst() : UNKNOWN_HOLDER;
  }

  private StatementSummary summary(StoredStatementSummary stored) {
    return jsonMapper.readValue(stored.getSummary(), StatementSummary.class);
  }

  static Totals totals(List<StatementSummary> summaries) {
    BigDecimal income = sum(summaries, s -> s.income().averageMonthlyTotal());
    BigDecimal obligations = sum(summaries, StatementSummary::monthlyObligations);
    return new Totals(
        income,
        obligations,
        sum(summaries, StatementSummary::monthlyCardRepayments),
        sum(summaries, StatementSummary::monthlySavings),
        income.signum() > 0 ? obligations.divide(income, 2, HALF_UP) : null);
  }

  private static BigDecimal sum(
      List<StatementSummary> summaries, Function<StatementSummary, BigDecimal> value) {
    return summaries.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
