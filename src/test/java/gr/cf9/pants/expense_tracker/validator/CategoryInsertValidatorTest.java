package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryCreateDTO;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CategoryInsertValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryInsertValidator validator;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsDuplicateRootCategoryUsingNormalizedName() {
        CategoryCreateDTO dto =
                new CategoryCreateDTO("  Food  ", TransactionType.EXPENSE, null);
        MapBindingResult errors =
                new MapBindingResult(new HashMap<>(), "categoryCreateDTO");

        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                testUser, "FOOD", TransactionType.EXPENSE
        )).thenReturn(true);

        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("name"));
        verify(categoryRepository).existsCategoryByUserAndNameAndTypeAndParentIsNull(
                testUser, "FOOD", TransactionType.EXPENSE
        );
    }

    @Test
    void rejectsDuplicateChildCategoryUsingNormalizedName() {
        UUID parentUuid = UUID.randomUUID();
        Category parent = new Category();
        parent.setType(TransactionType.EXPENSE);

        CategoryCreateDTO dto =
                new CategoryCreateDTO("  Grocery  ", TransactionType.EXPENSE, parentUuid);
        MapBindingResult errors =
                new MapBindingResult(new HashMap<>(), "categoryCreateDTO");

        when(categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(parentUuid, testUser))
                .thenReturn(java.util.Optional.of(parent));
        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParent(
                testUser, "GROCERY", TransactionType.EXPENSE, parent
        )).thenReturn(true);

        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("name"));
        verify(categoryRepository).existsCategoryByUserAndNameAndTypeAndParent(
                testUser, "GROCERY", TransactionType.EXPENSE, parent
        );
    }
}