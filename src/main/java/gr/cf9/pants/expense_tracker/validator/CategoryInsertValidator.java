package gr.cf9.pants.expense_tracker.validator;

import gr.cf9.pants.expense_tracker.core.util.NameNormalizer;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryCreateDTO;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class CategoryInsertValidator implements Validator {

    private final CategoryRepository categoryRepository;

    @Override
    public boolean supports(Class<?> clazz) { return CategoryCreateDTO.class.equals(clazz); }

    @Override
    public void validate(Object target, Errors errors) {
        CategoryCreateDTO dto = (CategoryCreateDTO) target;
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (dto.name() == null || dto.name().trim().isBlank()) {
            errors.rejectValue("name", "field.required", "Category name is required");
            return;
        }

        String name = NameNormalizer.normalizeName(dto.name());
        if (dto.parentUuid() == null) {
            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(user, name, dto.type())) {
                errors.rejectValue("name", "category.root.exists", "Parent category already exists");
            }
        } else {
            var parentOpt = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(dto.parentUuid(), user);
            if (parentOpt.isEmpty()) {
                errors.rejectValue("parentUuid", "category.parent.notFound", "Parent category with uuid=" + dto.parentUuid() + " not found");
                return;
            }
            Category parent = parentOpt.get();

            if (parent.getType() != dto.type()) {
                errors.rejectValue("type", "category.type.mismatch", "Parent category type and child category type must be the same");
            }
            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParent(user, name, dto.type(), parent)) {
                errors.rejectValue("name", "category.child.exists", "Child category already exists under this parent");
            }
        }
    }
}