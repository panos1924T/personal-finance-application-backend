package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryCreateDTO;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryUpdateDTO;
import gr.cf9.pants.expense_tracker.mapper.CategoryMapper;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import gr.cf9.pants.expense_tracker.repository.TransactionRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCategoryStoresTrimmedUppercaseName() {
        UUID userUuid = UUID.randomUUID();
        User user = new User();
        CategoryCreateDTO categoryCreateDTO =
                new CategoryCreateDTO("  Food   ", TransactionType.EXPENSE, null);
        Category category = new Category();

        when(userRepository.findUserByUuidAndDeletedFalse(userUuid))
                .thenReturn(Optional.of(user));
        when(categoryMapper.toEntity(categoryCreateDTO, user)).thenReturn(category);
        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                user, "FOOD", TransactionType.EXPENSE
        )).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        categoryService.createCategory(categoryCreateDTO, userUuid);

        assertEquals("FOOD", category.getName());
    }

    @Test
    void updateCategoryStoresTrimmedUppercaseName() {
        UUID userUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        User user = new User();
        Category category = new Category();
        category.setName("OLD NAME");
        category.setType(TransactionType.EXPENSE);
        CategoryUpdateDTO dto = new CategoryUpdateDTO("  Food  ");

        when(userRepository.findUserByUuidAndDeletedFalse(userUuid))
                .thenReturn(Optional.of(user));
        when(categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, user))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                user, "FOOD", TransactionType.EXPENSE
        )).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        categoryService.updateCategory(categoryUuid, dto, userUuid);

        assertEquals("FOOD", category.getName());
    }
}
