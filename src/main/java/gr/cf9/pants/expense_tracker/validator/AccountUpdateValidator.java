package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.util.NameNormalizer;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountUpdateDTO;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountUpdateValidator {

    private final AccountRepository accountRepository;

    public void validate(UUID accountUuid, AccountUpdateDTO dto, Errors errors) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (dto.name() == null || dto.name().trim().isBlank()) {
            errors.rejectValue("name", "field.required", "Account name is required");
            return;
        }

        if (accountRepository.existsAccountByUserAndNameAndUuidNot(
                user, NameNormalizer.normalizeName(dto.name()), accountUuid)) {
            errors.rejectValue("name", "account.name.exists", "Account name is taken from another account");
        }
    }
}