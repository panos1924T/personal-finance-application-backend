package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDefaultsInitializer {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public void createDefaultCategories(User user) {
        List<Category> categories = DEFAULT_CATEGORIES.stream()
                .map(seed -> {
                    Category category = new Category();
                    category.setUser(user);
                    category.setName(seed.name());
                    category.setType(seed.type());
                    return category;
                })
                .toList();

        categoryRepository.saveAll(categories);
    }

    public void createDefaultCashAccount(User user) {
        Account cashDefault = new Account(
                "CASH",
                BigDecimal.ZERO,
                AccountType.LIQUIDITY,
                true,
                user
        );

        accountRepository.save(cashDefault);
    }

    private record ParentCategorySeed(String name, TransactionType type) {}

    private static final List<ParentCategorySeed> DEFAULT_CATEGORIES = List.of(
            new ParentCategorySeed("PERSONAL",       TransactionType.EXPENSE),
            new ParentCategorySeed("HOME",           TransactionType.EXPENSE),
            new ParentCategorySeed("BUYS",           TransactionType.EXPENSE),
            new ParentCategorySeed("TRANSPORTATION", TransactionType.EXPENSE),
            new ParentCategorySeed("BILLS",          TransactionType.EXPENSE),
            new ParentCategorySeed("OTHER",          TransactionType.EXPENSE),

            new ParentCategorySeed("SALARY",         TransactionType.INCOME),
            new ParentCategorySeed("BUSINESS",       TransactionType.INCOME),
            new ParentCategorySeed("INVESTMENT",     TransactionType.INCOME)
    );
}
