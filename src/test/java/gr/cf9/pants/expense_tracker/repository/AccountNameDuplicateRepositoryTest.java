package gr.cf9.pants.expense_tracker.repository;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.Role;
import gr.cf9.pants.expense_tracker.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AccountNameDuplicateRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void archivedAccountNameRemainsReserved() {
        Role role = new Role();
        role.setName("TEST_ROLE");
        role = roleRepository.saveAndFlush(role);

        User user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setDisplayName("Test User");
        user.setPassword("test-password");
        user.setRole(role);
        user = userRepository.saveAndFlush(user);

        Account account = new Account();
        account.setName("WALLET");
        account.setBalance(BigDecimal.ZERO);
        account.setAccountType(AccountType.LIQUIDITY);
        account.setUser(user);
        account = accountRepository.saveAndFlush(account);

        account.softDelete(Instant.now());
        accountRepository.saveAndFlush(account);

        assertTrue(accountRepository.existsAccountByUserAndName(user, "WALLET"));
    }

    @Test
    void archivedRootCategoryNameRemainsReserved() {
        Role role = new Role();
        role.setName("CATEGORY_TEST_ROLE");
        role = roleRepository.saveAndFlush(role);

        User user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setDisplayName("Category Test");
        user.setPassword("test-password");
        user.setRole(role);
        user = userRepository.saveAndFlush(user);

        Category category = new Category();
        category.setName("FOOD");
        category.setType(TransactionType.EXPENSE);
        category.setUser(user);
        category = categoryRepository.saveAndFlush(category);

        category.softDelete(Instant.now());
        categoryRepository.saveAndFlush(category);

        assertTrue(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                user, "FOOD", TransactionType.EXPENSE
        ));
    }

    @Test
    void archivedChildCategoryNameRemainsReservedUnderSameParent() {
        Role role = new Role();
        role.setName("CHILD_CATEGORY_TEST_ROLE");
        role = roleRepository.saveAndFlush(role);

        User user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setDisplayName("Child Category Test");
        user.setPassword("test-password");
        user.setRole(role);
        user = userRepository.saveAndFlush(user);

        Category parent = new Category();
        parent.setName("FOOD");
        parent.setType(TransactionType.EXPENSE);
        parent.setUser(user);
        parent = categoryRepository.saveAndFlush(parent);

        Category child = new Category();
        child.setName("GROCERY");
        child.setType(TransactionType.EXPENSE);
        child.setUser(user);
        child.setParent(parent);
        child = categoryRepository.saveAndFlush(child);

        child.softDelete(Instant.now());
        categoryRepository.saveAndFlush(child);

        assertTrue(categoryRepository.existsCategoryByUserAndNameAndTypeAndParent(
                user, "GROCERY", TransactionType.EXPENSE, parent
        ));
    }
}