package gr.cf9.pants.expense_tracker.mapper;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountMapperTest {

    @Test
    void normalizesAccountNameWhenMappingCreateDTO() {
        AccountMapper accountMapper = new AccountMapper();
        AccountCreateDTO createDTO = new AccountCreateDTO(
                "    My SavIngs   ",
                AccountType.SAVINGS,
                null
        );

        Account account = accountMapper.toEntity(createDTO, new User());

        assertEquals("MY SAVINGS", account.getName());
    }
}
