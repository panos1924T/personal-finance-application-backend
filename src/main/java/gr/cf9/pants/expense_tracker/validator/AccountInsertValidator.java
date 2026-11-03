package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.util.NameNormalizer;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class AccountInsertValidator implements Validator {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return AccountCreateDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AccountCreateDTO dto = (AccountCreateDTO) target;
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (dto.name() == null || dto.name().trim().isBlank()) {
            errors.rejectValue("name", "field.required", "Account name is required");
            return;
        }

        if (accountRepository.existsAccountByUserAndName(user, NameNormalizer.normalizeName(dto.name()))) {
            errors.rejectValue("name", "account.name.exists", "Account name already exists");
        }
    }
}