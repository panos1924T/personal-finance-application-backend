package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.util.NameNormalizer;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryUpdateDTO;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryUpdateValidator {

    private final CategoryRepository categoryRepository;

    public void validate(UUID categoryUuid, CategoryUpdateDTO dto, Errors errors) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        var categoryOptional = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, user);
        if (categoryOptional.isEmpty()) {
            errors.rejectValue("name", "category.notFound", "Category with uuid=" + categoryUuid + " not found");
            return;
        }
        if (dto.name() == null || dto.name().trim().isBlank()) {
            errors.rejectValue("name", "field.required", "Category name is required");
            return;
        }

        Category category = categoryOptional.get();
        String newName = NameNormalizer.normalizeName(dto.name());

        if (category.getParent() == null) {
            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNullAndUuidNot(user, newName, category.getType(), categoryUuid)) {
                errors.rejectValue("name", "category.exists", "Parent category already exists");
            }
        } else {
            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParentAndUuidNot(user, newName, category.getType(), category.getParent(), categoryUuid)) {
                errors.rejectValue("name", "category.exists", "Child category already exists under this parent");
            }
        }
    }
}