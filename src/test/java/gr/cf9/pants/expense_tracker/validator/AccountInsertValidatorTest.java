package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.dto.account_dto.AccountCreateDTO;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.MapBindingResult;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Account Insert Validator Tests")
class AccountInsertValidatorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountInsertValidator validator;

    private User testUser;
    private AccountCreateDTO createDTO;
    private MapBindingResult bindingResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        createDTO = new AccountCreateDTO(
                "Test Account",
                null,
                new BigDecimal("100.00")
        );

        bindingResult = new MapBindingResult(new HashMap<>(), "accountCreateDTO");

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should support AccountCreateDTO class")
    void testSupports() {
        assertTrue(validator.supports(AccountCreateDTO.class));
        assertFalse(validator.supports(String.class));
    }

    @Test
    @DisplayName("Should reject when account name is null")
    void testValidate_NameIsNull() {
        AccountCreateDTO dtoWithNullName = new AccountCreateDTO(
                null,
                null,
                new BigDecimal("100.00")
        );

        validator.validate(dtoWithNullName, bindingResult);

        assertTrue(bindingResult.hasErrors());
        assertTrue(bindingResult.getFieldError("name").getDefaultMessage().contains("required"));
    }

    @Test
    @DisplayName("Should reject when account name is blank")
    void testValidate_NameIsBlank() {
        AccountCreateDTO dtoWithBlankName = new AccountCreateDTO(
                "   ",
                null,
                new BigDecimal("100.00")
        );

        validator.validate(dtoWithBlankName, bindingResult);

        assertTrue(bindingResult.hasErrors());
    }

    @Test
    @DisplayName("Should reject when account name already exists for user")
    void testValidate_NameAlreadyExists() {
        AccountCreateDTO dtoWithPaddedName = new AccountCreateDTO(
                "   Test AccOunt  ",
                null,
                new BigDecimal("100.00")
        );

        when(accountRepository.existsAccountByUserAndName(testUser, "TEST ACCOUNT"))
                .thenReturn(true);
        validator.validate(dtoWithPaddedName, bindingResult);

        assertTrue(bindingResult.hasErrors());
        assertTrue(bindingResult.getFieldError("name").getDefaultMessage().contains("exists"));
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void testValidate_Success() {
        when(accountRepository.existsAccountByUserAndName(testUser, "TEST ACCOUNT"))
                .thenReturn(false);

        validator.validate(createDTO, bindingResult);

        assertFalse(bindingResult.hasErrors());
    }
}
