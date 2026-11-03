package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.dto.account_dto.AccountUpdateDTO;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AccountUpdateValidatorTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AccountUpdateValidator validator;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void rejectsDuplicateNameAfterNormalization() {
        UUID accountUuid = UUID.randomUUID();
        AccountUpdateDTO dto = new AccountUpdateDTO(
                "   wAllEt   ",
                new BigDecimal("100.00")
        );
        MapBindingResult errors =
                new MapBindingResult(new HashMap<>(), "accountUpdateDTO");

        when(accountRepository.existsAccountByUserAndNameAndUuidNot(
                testUser,
                "WALLET",
                accountUuid
        )).thenReturn(true);

        validator.validate(accountUuid, dto, errors);

        assertTrue(errors.hasErrors());
        verify(accountRepository).existsAccountByUserAndNameAndUuidNot(
                testUser,
                "WALLET",
                accountUuid
        );
    }
}
