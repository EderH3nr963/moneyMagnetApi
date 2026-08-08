package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioTheme;
import com.moneyMagnetApi.demo.repository.projection.MonthlyFinancialProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldFindWithFiltersWhenAllFieldsAreProvided() {
        Usuario user = persistUser("usuario_filtros_completos");
        Account selected = persistAccount(user, "selected-account");
        Account another = persistAccount(user, "another-account");
        Transaction expected = persistTransaction(selected, "100.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        persistTransaction(selected, "-25.00", TransactionType.DEBIT, "2025-01-16T10:00:00");
        persistTransaction(selected, "200.00", TransactionType.CREDIT, "2025-02-01T10:00:00");
        persistTransaction(another, "300.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        flushAndClear();

        Page<Transaction> result = findWithFilters(
                user, selected, LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"),
                TransactionNature.INCOME, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(expected.getId(), result.getContent().get(0).getId());
    }

    @Test
    void shouldFindWithFiltersWhenStartDateIsNull() {
        Usuario user = persistUser("usuario_sem_data_inicial");
        Account account = persistAccount(user, "without-start-date");
        Transaction older = persistTransaction(account, "10.00", TransactionType.CREDIT, "2024-12-15T10:00:00");
        Transaction newer = persistTransaction(account, "20.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        persistTransaction(account, "30.00", TransactionType.CREDIT, "2025-02-01T10:00:00");
        flushAndClear();

        Page<Transaction> result = findWithFilters(
                user, account, null, LocalDate.parse("2025-01-31"), TransactionNature.INCOME, null);

        assertEquals(2, result.getTotalElements());
        assertTrue(ids(result).containsAll(List.of(older.getId(), newer.getId())));
    }

    @Test
    void shouldFindWithFiltersWhenEndDateIsNull() {
        Usuario user = persistUser("usuario_sem_data_final");
        Account account = persistAccount(user, "without-end-date");
        persistTransaction(account, "10.00", TransactionType.CREDIT, "2024-12-15T10:00:00");
        Transaction older = persistTransaction(account, "20.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        Transaction newer = persistTransaction(account, "30.00", TransactionType.CREDIT, "2025-02-01T10:00:00");
        flushAndClear();

        Page<Transaction> result = findWithFilters(
                user, account, LocalDate.parse("2025-01-01"), null, TransactionNature.INCOME, null);

        assertEquals(2, result.getTotalElements());
        assertTrue(ids(result).containsAll(List.of(older.getId(), newer.getId())));
    }

    @Test
    void shouldFindWithPostgreSqlFullTextSearch() {
        Usuario user = persistUser("usuario_busca_textual");
        Account account = persistAccount(user, "full-text-search");
        Transaction expected = persistTransaction(account, "-80.00", TransactionType.DEBIT, "2025-01-15T10:00:00");
        expected.setDescription("Compra no mercado central");
        Transaction ignored = persistTransaction(account, "-30.00", TransactionType.DEBIT, "2025-01-16T10:00:00");
        ignored.setDescription("Corrida de aplicativo");
        flushAndClear();

        Page<Transaction> result = findWithFilters(
                user, account, null, null, TransactionNature.EXPENSE, "mercado");

        assertEquals(1, result.getTotalElements());
        assertEquals(expected.getId(), result.getContent().get(0).getId());
    }

    @Test
    void shouldFindAllByUserAndInstitutionAndAccountTypeWhenFieldsAreCorrect() {
        Usuario user = persistUser("usuario_por_instituicao");
        Account expectedAccount = persistAccount(user, "institution-filter");
        Account anotherInstitution = persistAccount(user, "other-institution");
        Account anotherType = persistAccount(user, "other-account-type");
        anotherType.setType(AccountType.SAVINGS);
        entityManager.persistAndFlush(anotherType);
        Transaction expected = persistTransaction(expectedAccount, "100.00", TransactionType.CREDIT, "2025-01-10T10:00:00");
        persistTransaction(expectedAccount, "-10.00", TransactionType.DEBIT, "2025-01-11T10:00:00");
        persistTransaction(anotherInstitution, "200.00", TransactionType.CREDIT, "2025-01-12T10:00:00");
        persistTransaction(anotherType, "300.00", TransactionType.CREDIT, "2025-01-13T10:00:00");
        flushAndClear();

        Page<Transaction> result = transactionRepository.findAllByUserAndInstitutionAndAccountType(
                user.getId(), expectedAccount.getItem().getInstitution().getId(), AccountType.CHECKING,
                List.of(TransactionNature.INCOME), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(expected.getId(), result.getContent().get(0).getId());
    }

    @Test
    void shouldFindAllByUserAndItemAndAccountTypeWhenFieldsAreCorrect() {
        Usuario user = persistUser("usuario_por_item");
        Account expectedAccount = persistAccount(user, "item-filter");
        Account anotherItem = persistAccount(user, "other-item");
        Transaction expected = persistTransaction(expectedAccount, "-100.00", TransactionType.DEBIT, "2025-01-10T10:00:00");
        persistTransaction(expectedAccount, "10.00", TransactionType.CREDIT, "2025-01-11T10:00:00");
        persistTransaction(anotherItem, "-200.00", TransactionType.DEBIT, "2025-01-12T10:00:00");
        flushAndClear();

        Page<Transaction> result = transactionRepository.findAllByUserAndItemAndAccountType(
                user.getId(), expectedAccount.getItem().getId(), AccountType.CHECKING,
                List.of(TransactionNature.EXPENSE), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(expected.getId(), result.getContent().get(0).getId());
    }

    @Test
    void shouldFindAllByUserAndPeriodWhenFieldsAreCorrect() {
        Usuario user = persistUser("usuario_por_periodo");
        Account account = persistAccount(user, "period-filter");
        Account anotherUser = persistAccount(persistUser("outro_usuario_periodo"), "other-user-period");
        Transaction start = persistTransaction(account, "10.00", TransactionType.CREDIT, "2025-01-01T00:00:00");
        Transaction newer = persistTransaction(account, "20.00", TransactionType.CREDIT, "2025-01-31T23:59:59");
        Transaction older = persistTransaction(account, "30.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        persistTransaction(account, "40.00", TransactionType.CREDIT, "2025-02-01T00:00:01");
        persistTransaction(anotherUser, "50.00", TransactionType.CREDIT, "2025-01-15T10:00:00");
        flushAndClear();

        List<Transaction> result = transactionRepository.findAllByUserAndPeriod(
                user.getId(), LocalDateTime.parse("2025-01-01T00:00:00"),
                LocalDateTime.parse("2025-02-01T00:00:00"));

        assertEquals(List.of(newer.getId(), older.getId(), start.getId()),
                result.stream().map(Transaction::getId).toList());
    }

    @Test
    void shouldReturnMonthlyFinancialProjection() {
        Usuario user = persistUser("eder_teste");
        Account account = persistAccount(user, "account-1");
        persistTransaction(account, "100.00", TransactionType.CREDIT, "2025-01-05T10:00:00");
        persistTransaction(account, "-25.50", TransactionType.DEBIT, "2025-01-20T10:00:00");
        persistTransaction(account, "40.00", TransactionType.DEBIT, "2025-02-10T10:00:00");
        persistTransaction(account, "75.25", TransactionType.CREDIT, "2025-02-15T10:00:00");
        Account another = persistAccount(persistUser("outro_usuario"), "account-2");
        persistTransaction(another, "999.00", TransactionType.CREDIT, "2025-01-10T10:00:00");
        persistTransaction(account, "888.00", TransactionType.CREDIT, "2024-12-31T23:59:59");
        persistTransaction(account, "777.00", TransactionType.CREDIT, "2026-01-01T00:00:01");
        flushAndClear();

        List<MonthlyFinancialProjection> result = transactionRepository.findMonthlyFinancialSummary(
                user.getId(), LocalDateTime.parse("2025-01-01T00:00:00"),
                LocalDateTime.parse("2026-01-01T00:00:00"), TransactionType.CREDIT, TransactionType.DEBIT);
        Map<YearMonth, IncomeAndExpense> expected = Map.of(
                YearMonth.of(2025, 1), new IncomeAndExpense(new BigDecimal("100.00"), new BigDecimal("25.50")),
                YearMonth.of(2025, 2), new IncomeAndExpense(new BigDecimal("75.25"), new BigDecimal("40.00")));

        assertEquals(expected.size(), result.size());
        for (MonthlyFinancialProjection projection : result) {
            IncomeAndExpense values = expected.get(
                    YearMonth.of(projection.getYear().intValue(), projection.getMonth().intValue()));
            assertNotNull(values);
            assertBigDecimalEquals(values.income(), projection.getIncome());
            assertBigDecimalEquals(values.expense(), projection.getExpenses());
        }
    }

    @Test
    void shouldReturnEmptyProjectionWhenThereAreNoTransactionsInPeriod() {
        Usuario user = persistUser("usuario_sem_transacoes");
        List<MonthlyFinancialProjection> result = transactionRepository.findMonthlyFinancialSummary(
                user.getId(), LocalDateTime.parse("2025-01-01T00:00:00"),
                LocalDateTime.parse("2026-01-01T00:00:00"), TransactionType.CREDIT, TransactionType.DEBIT);
        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }

    private Page<Transaction> findWithFilters(
            Usuario user, Account account, LocalDate start, LocalDate end,
            TransactionNature nature, String search
    ) {
        return transactionRepository.findWithFilters(
                user.getId(), List.of(nature.toString()), account.getId(), start, end, search,
                PageRequest.of(0, 10));
    }

    private Usuario persistUser(String username) {
        Usuario user = new Usuario();
        user.setUsername(username);
        user.setEmail(username + "@email.com");
        user.setPassword("senha_teste");
        user.setRole(UsuarioRole.USER);
        user.setTheme(UsuarioTheme.LIGHT);
        return entityManager.persistAndFlush(user);
    }

    private Account persistAccount(Usuario user, String suffix) {
        Institution institution = new Institution();
        institution.setPluggyConnectorId("connector-" + suffix);
        institution.setName("Instituição " + suffix);
        institution = entityManager.persistAndFlush(institution);
        Item item = new Item();
        item.setUsuario(user);
        item.setInstitution(institution);
        item.setPluggyItemId("item-" + suffix);
        item.setStatus("UPDATED");
        item = entityManager.persistAndFlush(item);
        Account account = new Account();
        account.setItem(item);
        account.setPluggyAccountId(suffix);
        account.setName("Conta " + suffix);
        account.setType(AccountType.CHECKING);
        account.setCurrency("BRL");
        account.setBalance(BigDecimal.ZERO);
        return entityManager.persistAndFlush(account);
    }

    private Transaction persistTransaction(Account account, String amount, TransactionType type, String date) {
        LocalDateTime transactionDate = LocalDateTime.parse(date);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setDescription("Transação de teste");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCurrency("BRL");
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setNature(type == TransactionType.CREDIT ? TransactionNature.INCOME : TransactionNature.EXPENSE);
        transaction.setDate(transactionDate);
        transaction.setPaymentDate(transactionDate);
        return entityManager.persist(transaction);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<UUID> ids(Page<Transaction> page) {
        return page.getContent().stream().map(Transaction::getId).toList();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

    private record IncomeAndExpense(BigDecimal income, BigDecimal expense) {
    }
}
