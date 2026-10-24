package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.core.exceptions.EntityNotFoundException;
import gr.cf9.pants.expense_tracker.core.exceptions.InvalidArgumentException;
import gr.cf9.pants.expense_tracker.core.filters.AccountFilters;
import gr.cf9.pants.expense_tracker.core.filters.CategoryFilters;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountReadOnlyDTO;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountUpdateDTO;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryReadOnlyDTO;
import gr.cf9.pants.expense_tracker.mapper.AccountMapper;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import gr.cf9.pants.expense_tracker.specification.AccountSpecification;
import gr.cf9.pants.expense_tracker.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService implements IAccountService{

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Transactional
    @Override
    public AccountReadOnlyDTO createAccount(AccountCreateDTO dto, UUID userUuid) {

        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));

        //PREPARE
        Account account = accountMapper.toEntity(dto, user);

        //EXECUTE
        Account savedAccount = accountRepository.save(account);

        //RETURN
        return accountMapper.toReadOnly(savedAccount);
    }

    @Transactional
    @Override
    public AccountReadOnlyDTO updateAccount(UUID accountUuid, AccountUpdateDTO dto, UUID userUuid) {

        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Account account = accountRepository.findAccountByUuidAndUserAndDeletedFalse(accountUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account", "Account with uuid: " + accountUuid + " not found!"));

        if (account.isDefaultAccount()) {
            throw new InvalidArgumentException("Account", "Cannot update default account");
        }

        //PREPARE
        account.setName(dto.name());

        //EXECUTE
        Account updatedAccount = accountRepository.save(account);

        //RETURN
        return accountMapper.toReadOnly(updatedAccount);
    }

    @Transactional
    @Override
    public void deleteAccount(UUID accountUuid, UUID userUuid) {

        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Account account = accountRepository.findAccountByUuidAndUser(accountUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account", "Account with uuid: " + accountUuid + " not found!"));

        if (account.isDeleted() == true) {
            throw new InvalidArgumentException("Account", "Account with uuid:" + accountUuid + " is already deleted");
        }

        if (account.isDefaultAccount()) {
            throw new InvalidArgumentException("Account", "Cannot delete default account!");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidArgumentException("account", "Cannot delete account with non-zero balance");
        }

        account.softDelete(Instant.now());
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AccountReadOnlyDTO> getFilteredAndPaginatedAccounts(UUID userUuid, AccountFilters filters, Pageable pageable) {
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + userUuid + " not found"));

        var specification = AccountSpecification.build(filters, user);

        return accountRepository.findAll(specification, pageable)
                .map(accountMapper::toReadOnly);
    }

    //    @Override
//    @Transactional(readOnly = true)
//    public List<AccountReadOnlyDTO> getAllAccounts(UUID userUuid) {
//
//        //VALIDATE
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        //PREPARE
//        List<Account> accounts = accountRepository.findAccountByUser(user);
//
//        //EXECUTE AND RETURN
//        return accounts.stream()
//                .map(accountMapper::toReadOnly)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<AccountReadOnlyDTO> getActiveAccounts(UUID userUuid) {
//        //VALIDATE
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        //PREPARE
//        List<Account> accounts = accountRepository.findAccountByUserAndDeletedFalse(user);
//
//        //EXECUTE AND RETURN
//        return accounts.stream()
//                .map(accountMapper::toReadOnly)
//                .toList();
//    }
//
    @Override
    @Transactional(readOnly = true)
    public AccountReadOnlyDTO getActiveAccountByUuid(UUID accountUuid, UUID userUuid) {

        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Account account = accountRepository.findAccountByUuidAndUserAndDeletedFalse(accountUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account", "Account with uuid: " + accountUuid + " not found!"));

        //RETURN
        return accountMapper.toReadOnly(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReadOnlyDTO getAccountByUuid(UUID accountUuid, UUID userUuid) {
        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Account account = accountRepository.findAccountByUuidAndUser(accountUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account", "Account with uuid: " + accountUuid + " not found!"));

        //RETURN
        return accountMapper.toReadOnly(account);
    }
//
//    @Override
//    public List<AccountReadOnlyDTO> getAccountsByType(UUID userUuid, AccountType accountType) {
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + "  not found!"));
//
//        List<Account> accounts = accountRepository.findAccountByUserAndAccountType(user, accountType);
//
//        return accounts.stream()
//                .map(accountMapper::toReadOnly)
//                .toList();
//    }
//
//    @Override
//    public List<AccountReadOnlyDTO> getActiveAccountsByType(UUID userUuid, AccountType accountType) {
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        List<Account> accounts = accountRepository.findAccountByUserAndAccountTypeAndDeletedFalse(user, accountType);
//
//        return accounts.stream()
//                .map(accountMapper::toReadOnly)
//                .toList();
//    }
}
