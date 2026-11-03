package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryUpdateDTO;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CategoryUpdateValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryUpdateValidator validator;

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
        UUID categoryUuid = UUID.randomUUID();
        Category category = new Category();
        category.setName("OLD NAME");
        category.setType(TransactionType.EXPENSE);

        CategoryUpdateDTO dto = new CategoryUpdateDTO("  Food  ");
        MapBindingResult errors =
                new MapBindingResult(new HashMap<>(), "categoryUpdateDTO");

        when(categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, testUser))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNullAndUuidNot(
                testUser, "FOOD", TransactionType.EXPENSE, categoryUuid
        )).thenReturn(true);

        validator.validate(categoryUuid, dto, errors);

        assertTrue(errors.hasFieldErrors("name"));
        verify(categoryRepository).existsCategoryByUserAndNameAndTypeAndParentIsNullAndUuidNot(
                testUser, "FOOD", TransactionType.EXPENSE, categoryUuid
        );
    }

    @Test
    void rejectsDuplicateChildCategoryUsingNormalizedName() {
        UUID categoryUuid = UUID.randomUUID();
        Category parent = new Category();
        parent.setType(TransactionType.EXPENSE);

        Category child = new Category();
        child.setName("OLD NAME");
        child.setType(TransactionType.EXPENSE);
        child.setParent(parent);

        CategoryUpdateDTO dto = new CategoryUpdateDTO("  Grocery  ");
        MapBindingResult errors =
                new MapBindingResult(new HashMap<>(), "categoryUpdateDTO");

        when(categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, testUser))
                .thenReturn(Optional.of(child));
        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentAndUuidNot(
                testUser, "GROCERY", TransactionType.EXPENSE, parent, categoryUuid
        )).thenReturn(true);

        validator.validate(categoryUuid, dto, errors);

        assertTrue(errors.hasFieldErrors("name"));
        verify(categoryRepository).existsCategoryByUserAndNameAndTypeAndParentAndUuidNot(
                testUser, "GROCERY", TransactionType.EXPENSE, parent, categoryUuid
        );
    }
}