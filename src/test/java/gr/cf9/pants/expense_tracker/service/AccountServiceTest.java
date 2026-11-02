package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.core.exceptions.EntityNotFoundException;
import gr.cf9.pants.expense_tracker.core.exceptions.InvalidArgumentException;
import gr.cf9.pants.expense_tracker.core.filters.AccountFilters;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountReadOnlyDTO;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountUpdateDTO;
import gr.cf9.pants.expense_tracker.mapper.AccountMapper;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Account Service Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;
    private AccountCreateDTO createDTO;
    private AccountUpdateDTO updateDTO;
    private AccountReadOnlyDTO readOnlyDTO;
    private UUID testUserUuid;
    private UUID testAccountUuid;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUserUuid = UUID.randomUUID();
        testAccountUuid = UUID.randomUUID();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUuid(testUserUuid);
        testUser.setEmail("test@example.com");
        testUser.setDeleted(false);

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUuid(testAccountUuid);
        testAccount.setName("My Savings");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(new BigDecimal("1000.00")); // Αρχικό balance
        testAccount.setUser(testUser);
        testAccount.setDefaultAccount(false);
        testAccount.setDeleted(false);
        testAccount.setCreatedAt(Instant.now());

        createDTO = new AccountCreateDTO(
                "My Savings",
                AccountType.SAVINGS,
                new BigDecimal("1000.00")
        );

        updateDTO = new AccountUpdateDTO(
                "Updated Savings",
                new BigDecimal("1500.00")
        );

        readOnlyDTO = new AccountReadOnlyDTO(
                testAccountUuid,
                "My Savings",
                "SAVINGS",
                new BigDecimal("1000.00"),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("CREATE ACCOUNT Tests")
    class CreateAccountTests {

        @Test
        @DisplayName("✅ Should create account successfully when user exists")
        void testCreateAccount_Success() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountMapper.toEntity(createDTO, testUser))
                    .thenReturn(testAccount);
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(testAccount);
            when(accountMapper.toReadOnly(testAccount))
                    .thenReturn(readOnlyDTO);

            AccountReadOnlyDTO result = accountService.createAccount(createDTO, testUserUuid);

            assertNotNull(result);
            assertEquals("My Savings", result.name());
            assertEquals(AccountType.SAVINGS.name(), result.accountType());
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("❌ Should throw EntityNotFoundException when user does not exist")
        void testCreateAccount_UserNotFound() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> accountService.createAccount(createDTO, testUserUuid)
            );

            assertTrue(exception.getMessage().contains("not found"));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("✅ Should create account with zero balance when initialBalance is null")
        void testCreateAccount_WithNullBalance() {
            Account accountWithZeroBalance = new Account();
            accountWithZeroBalance.setBalance(BigDecimal.ZERO);
            accountWithZeroBalance.setName("My Wallet");
            accountWithZeroBalance.setAccountType(AccountType.LIQUIDITY);
            accountWithZeroBalance.setUser(testUser);

            AccountReadOnlyDTO dtoZeroBalance = new AccountReadOnlyDTO(
                    testAccountUuid,
                    "My Wallet",
                    "LIQUIDITY",
                    BigDecimal.ZERO,
                    Instant.now()
            );

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountMapper.toEntity(any(AccountCreateDTO.class), eq(testUser)))
                    .thenReturn(accountWithZeroBalance);
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(accountWithZeroBalance);
            when(accountMapper.toReadOnly(accountWithZeroBalance))
                    .thenReturn(dtoZeroBalance);

            AccountReadOnlyDTO result = accountService.createAccount(
                    new AccountCreateDTO("My Wallet", AccountType.LIQUIDITY, null),
                    testUserUuid
            );

            assertEquals(BigDecimal.ZERO, result.balance());
        }
    }

    @Nested
    @DisplayName("UPDATE ACCOUNT Tests")
    class UpdateAccountTests {

        @Test
        @DisplayName("✅ Should update account successfully")
        void testUpdateAccount_Success() {
            Account updatedAccount = new Account();
            updatedAccount.setUuid(testAccountUuid);
            updatedAccount.setName("UPDATED SAVINGS");
            updatedAccount.setBalance(new BigDecimal("1500.00"));
            updatedAccount.setDefaultAccount(false);
            updatedAccount.setUser(testUser);

            AccountReadOnlyDTO updatedDTO = new AccountReadOnlyDTO(
                    testAccountUuid,
                    "UPDATED SAVINGS",
                    "SAVINGS",
                    new BigDecimal("1500.00"),
                    Instant.now()
            );

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(updatedAccount);
            when(accountMapper.toReadOnly(updatedAccount))
                    .thenReturn(updatedDTO);

            AccountUpdateDTO paddedNameDto = new AccountUpdateDTO(
                    "  Updated Savings  ",
                    updateDTO.balance()
            );

            AccountReadOnlyDTO result = accountService.updateAccount(testAccountUuid, paddedNameDto, testUserUuid);

            assertNotNull(result);
            assertEquals("UPDATED SAVINGS", testAccount.getName());
            assertEquals("UPDATED SAVINGS", result.name());
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("❌ Should not update default account")
        void testUpdateAccount_DefaultAccountError() {
            testAccount.setDefaultAccount(true);

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));

            InvalidArgumentException exception = assertThrows(
                    InvalidArgumentException.class,
                    () -> accountService.updateAccount(testAccountUuid, updateDTO, testUserUuid)
            );

            assertTrue(exception.getMessage().contains("default account"));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("❌ Should throw EntityNotFoundException when account not found")
        void testUpdateAccount_AccountNotFound() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> accountService.updateAccount(testAccountUuid, updateDTO, testUserUuid)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("DELETE ACCOUNT Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("✅ Should delete account successfully")
        void testDeleteAccount_Success() {
            // Μηδενισμός του balance ειδικά για αυτό το test ώστε να περάσει
            testAccount.setBalance(BigDecimal.ZERO);

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(testAccount);

            assertDoesNotThrow(() -> accountService.deleteAccount(testAccountUuid, testUserUuid));
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("❌ Should not delete default account")
        void testDeleteAccount_DefaultAccountError() {
            testAccount.setDefaultAccount(true);
            testAccount.setBalance(BigDecimal.ZERO); // Καλό είναι να είναι μηδέν και εδώ, για να χτυπήσει το default πρώτα

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));

            InvalidArgumentException exception = assertThrows(
                    InvalidArgumentException.class,
                    () -> accountService.deleteAccount(testAccountUuid, testUserUuid)
            );

            assertTrue(exception.getMessage().contains("default account"));
            verify(accountRepository, never()).save(any(Account.class));
        }
    }


    @Test
    @DisplayName("❌ Should not delete account with non-zero balance")
    void testDeleteAccount_NonZeroBalanceError() {
        testAccount.setBalance(new BigDecimal("500.00")); // Βεβαιωνόμαστε ότι δεν είναι 0

        when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                .thenReturn(Optional.of(testUser));
        when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                .thenReturn(Optional.of(testAccount));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> accountService.deleteAccount(testAccountUuid, testUserUuid)
        );

        assertTrue(exception.getMessage().contains("non-zero balance"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("❌ Should not delete already deleted account")
    void testDeleteAccount_AlreadyDeletedError() {
        testAccount.setDeleted(true);
        testAccount.setBalance(BigDecimal.ZERO); // Αποφεύγουμε το non-zero exception

        when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                .thenReturn(Optional.of(testUser));
        when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                .thenReturn(Optional.of(testAccount));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> accountService.deleteAccount(testAccountUuid, testUserUuid)
        );

        assertTrue(exception.getMessage().contains("already deleted"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("❌ Should throw EntityNotFoundException when account not found")
    void testDeleteAccount_AccountNotFound() {
        when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                .thenReturn(Optional.of(testUser));
        when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> accountService.deleteAccount(testAccountUuid, testUserUuid)
        );

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Nested
    @DisplayName("GET ACCOUNT Tests")
    class GetAccountTests {

        @Test
        @DisplayName("✅ Should retrieve active account by UUID successfully")
        void testGetActiveAccountByUuid_Success() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));
            when(accountMapper.toReadOnly(testAccount))
                    .thenReturn(readOnlyDTO);

            AccountReadOnlyDTO result = accountService.getActiveAccountByUuid(testAccountUuid, testUserUuid);

            assertNotNull(result);
            assertEquals("My Savings", result.name());
            verify(accountRepository, times(1)).findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser);
        }

        @Test
        @DisplayName("❌ Should throw EntityNotFoundException when active account not found")
        void testGetActiveAccountByUuid_NotFound() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUserAndDeletedFalse(testAccountUuid, testUser))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> accountService.getActiveAccountByUuid(testAccountUuid, testUserUuid)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("✅ Should retrieve account by UUID (including deleted)")
        void testGetAccountByUuid_Success() {
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAccountByUuidAndUser(testAccountUuid, testUser))
                    .thenReturn(Optional.of(testAccount));
            when(accountMapper.toReadOnly(testAccount))
                    .thenReturn(readOnlyDTO);

            AccountReadOnlyDTO result = accountService.getAccountByUuid(testAccountUuid, testUserUuid);

            assertNotNull(result);
            assertEquals("My Savings", result.name());
            verify(accountRepository, times(1)).findAccountByUuidAndUser(testAccountUuid, testUser);
        }

        @Test
        @DisplayName("✅ Should retrieve paginated and filtered accounts")
        void testGetFilteredAndPaginatedAccounts_Success() {
            AccountFilters filters = AccountFilters.builder()
                    .name("Savings")
                    .accountType(AccountType.SAVINGS)
                    .includeDeleted(false)
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount), pageable, 1);

            // Mocks
            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.of(testUser));
            when(accountRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(accountPage);

            when(accountMapper.toReadOnly(testAccount))
                    .thenReturn(readOnlyDTO);

            // Εκτέλεση
            Page<AccountReadOnlyDTO> result = accountService.getFilteredAndPaginatedAccounts(
                    testUserUuid,
                    filters,
                    pageable
            );

            // Assertions
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("My Savings", result.getContent().get(0).name());
            verify(accountRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("❌ Should throw EntityNotFoundException when user not found for filtering")
        void testGetFilteredAndPaginatedAccounts_UserNotFound() {
            AccountFilters filters = new AccountFilters();
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.findUserByUuidAndDeletedFalse(testUserUuid))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> accountService.getFilteredAndPaginatedAccounts(testUserUuid, filters, pageable)
            );

            assertTrue(exception.getMessage().contains("not found"));
            verify(accountRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
        }
    }
}