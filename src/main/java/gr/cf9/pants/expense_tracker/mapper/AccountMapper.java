package gr.cf9.pants.expense_tracker.mapper;

import gr.cf9.pants.expense_tracker.core.util.NameNormalizer;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountReadOnlyDTO;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountMapper {

    public AccountReadOnlyDTO toReadOnly(Account account) {
        return new AccountReadOnlyDTO(
                account.getUuid(),
                account.getName(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }

    public Account toEntity(AccountCreateDTO dto, User user) {
        Account account = new Account();
        account.setUser(user);
        account.setName(NameNormalizer.normalizeName(dto.name()));
        account.setAccountType(dto.accountType());
        account.setBalance(
                dto.initialBalance() != null ? dto.initialBalance() : BigDecimal.ZERO
        );
        return account;
    }
}
