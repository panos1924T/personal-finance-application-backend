package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.dto.transaction_dto.TransactionUpdateDTO;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.Transaction;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import gr.cf9.pants.expense_tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionUpdateValidator {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public void validate(UUID transactionUuid, TransactionUpdateDTO dto, Errors errors) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Optional<Transaction> transactionOptional = transactionRepository.findTransByUuidAndUser(transactionUuid, user);
        if (transactionOptional.isEmpty()) {
            errors.reject("transaction.notFound", "Transaction with uuid=" + transactionUuid + " not found");
            return;
        }
        Transaction transaction = transactionOptional.get();

        switch (transaction.getType()) {
            case INCOME -> validateIncomeUpdate(transaction, dto, user, errors);
            case EXPENSE -> validateExpenseUpdate(transaction, dto, user, errors);
            case TRANSFER -> validateTransferUpdate(transaction, dto, user, errors);
        }
    }

    private void validateIncomeUpdate(Transaction transaction, TransactionUpdateDTO dto, User user, Errors errors) {
        validateCategoryUpdate(transaction, dto.categoryUuid(), user, errors);
    }

    private void validateExpenseUpdate(Transaction transaction, TransactionUpdateDTO dto, User user, Errors errors) {
        validateCategoryUpdate(transaction, dto.categoryUuid(), user, errors);
        validateAccountUpdate("sourceAccountUuid", transaction.getSourceAccount(), dto.sourceAccountUuid(), user, errors);
    }

    private void validateTransferUpdate(Transaction transaction, TransactionUpdateDTO dto, User user, Errors errors) {
        boolean isSourceAccountValid = validateAccountUpdate("sourceAccountUuid", transaction.getSourceAccount(), dto.sourceAccountUuid(), user, errors);
        boolean isTargetAccountValid = validateAccountUpdate("targetAccountUuid", transaction.getTargetAccount(), dto.targetAccountUuid(), user, errors);

        if (isSourceAccountValid && isTargetAccountValid && dto.sourceAccountUuid().equals(dto.targetAccountUuid())) {
            errors.rejectValue("targetAccountUuid", "transfer.same", "Source and Target account cannot be the same");
        }
    }


    private void validateCategoryUpdate(Transaction currentTransaction, UUID newCategoryUuid, User user, Errors errors) {
        if (newCategoryUuid == null) {
            errors.rejectValue("categoryUuid", "field.required", "Category is required");
            return;
        }

        if (!newCategoryUuid.equals(currentTransaction.getCategory().getUuid())) {
            Optional<Category> categoryOptional = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(newCategoryUuid, user);
            if (categoryOptional.isEmpty()) {
                errors.rejectValue("categoryUuid", "category.notFound", "Category with uuid=" + newCategoryUuid + " not found");
            } else if (categoryOptional.get().getType() != currentTransaction.getType()) {
                errors.rejectValue("categoryUuid", "category.invalid", "Category type must match transaction type " + currentTransaction.getType());
            }
        }
    }

    private boolean validateAccountUpdate(String fieldName, Account currentAccount, UUID newAccountUuid, User user, Errors errors) {
        if (newAccountUuid == null) {
            errors.rejectValue(fieldName, "field.required", "Account is required");
            return false;
        }

        if (!newAccountUuid.equals(currentAccount.getUuid())) {
            if (!accountRepository.existsAccountByUuidAndUserAndDeletedFalse(newAccountUuid, user)) {
                errors.rejectValue(fieldName, "account.notFound", "Updated account with uuid=" + newAccountUuid + " not found");
                return false;
            }
        }
        return true;
    }
}