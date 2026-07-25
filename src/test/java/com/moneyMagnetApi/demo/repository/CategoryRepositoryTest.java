package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioTheme;
import com.moneyMagnetApi.demo.dto.dashboard.response.CategoryExpenseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    private static final LocalDateTime START_DATE =
            LocalDateTime.parse("2025-01-01T00:00:00");
    private static final LocalDateTime END_DATE =
            LocalDateTime.parse("2025-02-01T00:00:00");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldGroupExpensesByCategoryAndOrderByHighestAmount() {
        Usuario user = persistUser("category_totals");
        Account account = persistAccount(user, "category-totals", AccountType.CHECKING);
        Category food = persistCategory(user, "Alimentação", "alimentacao", "#FF0000");
        Category transport = persistCategory(user, "Transporte", "transporte", "#0000FF");

        persistTransaction(account, food, "-40.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-10T10:00:00", null);
        persistTransaction(account, food, "-60.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-11T10:00:00", null);
        persistTransaction(account, transport, "-150.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-12T10:00:00", null);
        persistTransaction(account, null, "-25.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-13T10:00:00", null);

        entityManager.flush();
        entityManager.clear();

        List<CategoryExpenseResponse> result = findExpenses(user);

        assertEquals(3, result.size());
        assertCategoryExpense(result.get(0), transport, "150.00");
        assertCategoryExpense(result.get(1), food, "100.00");
        assertNull(result.get(2).categoryId());
        assertEquals("Sem categoria", result.get(2).categoryName());
        assertBigDecimalEquals("25.00", result.get(2).amount());
        assertEquals("#71717A", result.get(2).color());
    }

    @Test
    void shouldOnlyIncludePostedExpensesForUserInsidePeriod() {
        Usuario user = persistUser("category_filters");
        Account account = persistAccount(user, "category-filters", AccountType.CHECKING);
        Account anotherUserAccount = persistAccount(
                persistUser("category_other_user"), "category-other-user", AccountType.CHECKING);
        Category category = persistCategory(user, "Compras", "compras", "#00FF00");

        persistTransaction(account, category, "-50.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-15T10:00:00", null);
        persistTransaction(account, category, "-100.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.PENDING, "2025-01-15T10:00:00", null);
        persistTransaction(account, category, "200.00", TransactionType.CREDIT,
                TransactionNature.INCOME, TransactionStatus.POSTED, "2025-01-15T10:00:00", null);
        persistTransaction(account, category, "-300.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2024-12-31T23:59:59", null);
        persistTransaction(account, category, "-400.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-02-01T00:00:00", null);
        persistTransaction(anotherUserAccount, category, "-500.00", TransactionType.DEBIT,
                TransactionNature.EXPENSE, TransactionStatus.POSTED, "2025-01-15T10:00:00", null);

        entityManager.flush();
        entityManager.clear();

        List<CategoryExpenseResponse> result = findExpenses(user);

        assertEquals(1, result.size());
        assertCategoryExpense(result.get(0), category, "50.00");
    }

    @Test
    void shouldApplyLegacyRulesWhenTransactionNatureIsNull() {
        Usuario user = persistUser("category_legacy");
        Account checkingAccount = persistAccount(user, "legacy-checking", AccountType.CHECKING);
        Account creditAccount = persistAccount(user, "legacy-credit", AccountType.CREDIT);
        Category general = persistCategory(user, "Geral", "geral", "#123456");
        Category transfer = persistCategory(
                user, "Transferência entre contas", "transferencia-entre-contas", "#654321");

        persistTransaction(checkingAccount, general, "-80.00", TransactionType.DEBIT,
                null, TransactionStatus.POSTED, "2025-01-10T10:00:00", null);
        persistTransaction(checkingAccount, general, "90.00", TransactionType.CREDIT,
                null, TransactionStatus.POSTED, "2025-01-11T10:00:00", null);
        persistTransaction(checkingAccount, transfer, "-100.00", TransactionType.DEBIT,
                null, TransactionStatus.POSTED, "2025-01-12T10:00:00", null);
        persistTransaction(creditAccount, general, "-110.00", TransactionType.DEBIT,
                null, TransactionStatus.POSTED, "2025-01-13T10:00:00", "credit card payment");
        persistTransaction(creditAccount, general, "-20.00", TransactionType.DEBIT,
                null, TransactionStatus.POSTED, "2025-01-14T10:00:00", "Groceries");

        entityManager.flush();
        entityManager.clear();

        List<CategoryExpenseResponse> result = findExpenses(user);

        assertEquals(1, result.size());
        assertCategoryExpense(result.get(0), general, "100.00");
    }

    private List<CategoryExpenseResponse> findExpenses(Usuario user) {
        return categoryRepository.findExpensesByCategory(
                user.getId(),
                START_DATE,
                END_DATE,
                TransactionNature.EXPENSE,
                TransactionStatus.POSTED);
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

    private Account persistAccount(Usuario user, String suffix, AccountType type) {
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
        account.setPluggyAccountId("account-" + suffix);
        account.setName("Conta " + suffix);
        account.setType(type);
        account.setCurrency("BRL");
        account.setBalance(BigDecimal.ZERO);
        return entityManager.persistAndFlush(account);
    }

    private Category persistCategory(
            Usuario user,
            String name,
            String normalizedName,
            String color
    ) {
        Category category = new Category();
        category.setUsuario(user);
        category.setName(name);
        category.setNormalizedName(normalizedName);
        category.setColor(color);
        return entityManager.persistAndFlush(category);
    }

    private void persistTransaction(
            Account account,
            Category category,
            String amount,
            TransactionType type,
            TransactionNature nature,
            TransactionStatus status,
            String date,
            String providerCategory
    ) {
        LocalDateTime transactionDate = LocalDateTime.parse(date);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDescription("Transação de teste");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCurrency("BRL");
        transaction.setType(type);
        transaction.setNature(nature);
        transaction.setStatus(status);
        transaction.setDate(transactionDate);
        transaction.setPaymentDate(transactionDate);
        transaction.setProviderCategory(providerCategory);
        entityManager.persist(transaction);
    }

    private void assertCategoryExpense(
            CategoryExpenseResponse result,
            Category expectedCategory,
            String expectedAmount
    ) {
        assertEquals(expectedCategory.getId(), result.categoryId());
        assertEquals(expectedCategory.getName(), result.categoryName());
        assertEquals(expectedCategory.getColor(), result.color());
        assertBigDecimalEquals(expectedAmount, result.amount());
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
