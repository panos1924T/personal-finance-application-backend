package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.exceptions.EntityAlreadyExistsException;
import gr.cf9.pants.expense_tracker.core.exceptions.EntityNotFoundException;
import gr.cf9.pants.expense_tracker.core.exceptions.InvalidArgumentException;
import gr.cf9.pants.expense_tracker.core.filters.CategoryFilters;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryCreateDTO;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryReadOnlyDTO;
import gr.cf9.pants.expense_tracker.dto.category_dto.CategoryUpdateDTO;
import gr.cf9.pants.expense_tracker.mapper.CategoryMapper;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import gr.cf9.pants.expense_tracker.repository.TransactionRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import gr.cf9.pants.expense_tracker.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService implements ICategoryService{

    private final CategoryMapper categoryMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    @Override
    public CategoryReadOnlyDTO createCategory(CategoryCreateDTO dto, UUID userUuid) {
        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));

        String name = normalizeName(dto.name());

        if (dto.type() == null) {
            throw new InvalidArgumentException("Category", "Category type is required");
        }

        Category category = categoryMapper.toEntity(dto, user);
        category.setName(name);

        if (dto.parentUuid() == null) {
            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                    user,
                    name,
                    dto.type()
            )) {
                throw new EntityAlreadyExistsException("Category", "Root category already exists");
            }

            category.setParent(null);
        } else {
            Category parent = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(dto.parentUuid(), user)
                    .orElseThrow(() -> new EntityNotFoundException("Category", "Parent category with uuid: " + dto.parentUuid() + " not found!"));

            if (parent.getParent() != null) {
                throw new InvalidArgumentException("Category", "Child category cannot be used as parent");
            }

            if (parent.getType() != dto.type()) {
                throw new InvalidArgumentException("Category", "Child category type must match parent category type");
            }

            if (categoryRepository.existsCategoryByUserAndNameAndTypeAndParent(
                    user,
                    name,
                    dto.type(),
                    parent
            )) {
                throw new EntityAlreadyExistsException("Category", "Child category already exists under this parent");
            }

            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);

        //RETURN
        return categoryMapper.toReadOnly(savedCategory);
    }

    @Transactional
    @Override
    public CategoryReadOnlyDTO updateCategory(UUID categoryUuid, CategoryUpdateDTO dto, UUID userUuid) {

        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Category category = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category", "Category with uuid: " + categoryUuid + "not found!"));

        String name = normalizeName(dto.name());

        if (category.getParent() == null) {
            if (!category.getName().equals(name)
                    && categoryRepository.existsCategoryByUserAndNameAndTypeAndParentIsNull(
                    user,
                    name,
                    category.getType()
            )) {
                throw new EntityAlreadyExistsException("Category", "Root category already exists");
            }
        } else {
            if (!category.getName().equals(name)
                    && categoryRepository.existsCategoryByUserAndNameAndTypeAndParent(
                    user,
                    name,
                    category.getType(),
                    category.getParent()
            )) {
                throw new EntityAlreadyExistsException("Category", "Child category already exists under this parent");
            }
        }

        category.setName(name);

        //EXECUTE
        Category updatedCategory = categoryRepository.save(category);

        //RETURN
        return categoryMapper.toReadOnly(updatedCategory);
    }

    @Transactional
    @Override
    public void deleteCategory(UUID categoryUuid, UUID userUuid) {
        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Category category = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category", "Category with uuid: " + categoryUuid + "not found!"));

        if (category.getParent() == null && categoryRepository.existsCategoryByParentAndDeletedFalse(category)) {
            throw new InvalidArgumentException("Category", "Cannot delete parent category with active children");
        }

        category.softDelete(Instant.now());
        categoryRepository.save(category);

    }

    @Override
    public CategoryReadOnlyDTO getActiveCategoryByUuid(UUID categoryUuid, UUID userUuid) {
        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Category category = categoryRepository.findCategoryByUuidAndUserAndDeletedFalse(categoryUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category", "Category with uuid: " + categoryUuid + "not found!"));

        //RETURN
        return categoryMapper.toReadOnly(category);
    }

    @Override
    public CategoryReadOnlyDTO getCategoryByUuid(UUID categoryUuid, UUID userUuid) {
        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
        Category category = categoryRepository.findCategoryByUuidAndUser(categoryUuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category", "Category with uuid: " + categoryUuid + "not found!"));

        //RETURN
        return categoryMapper.toReadOnly(category);
    }
//
//    @Override
//    public List<CategoryReadOnlyDTO> getActiveCategoriesByType(TransactionType type, UUID userUuid) {
//        //VALIDATE
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        //PREPARE
//        List<Category> categories = categoryRepository.findCategoryByUserAndTypeAndDeletedFalse(user, type);
//
//        //EXECUTE & RETURN
//        return categories.stream()
//                .map(categoryMapper::toReadOnly)
//                .toList();
//    }
//
//    @Override
//    public List<CategoryReadOnlyDTO> getCategoriesByType(TransactionType type, UUID userUuid) {
//        //VALIDATE
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        //PREPARE
//        List<Category> categories = categoryRepository.findCategoryByUserAndType(user, type);
//
//        //EXECUTE & RETURN
//        return categories.stream()
//                .map(categoryMapper::toReadOnly)
//                .toList();
//    }
//
//    @Override
//    public List<CategoryReadOnlyDTO> getAllCategories(UUID userUuid) {
//        //VALIDATE
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        //PREPARE
//        List<Category> categories = categoryRepository.findCategoryByUser(user);
//
//        //EXECUTE & RETURN
//        return categories.stream()
//                .map(categoryMapper::toReadOnly)
//                .toList();
//    }
//
//    @Override
//    public List<CategoryReadOnlyDTO> getActiveCategories(UUID userUuid) {
//        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
//                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + userUuid + " not found!"));
//
//        List<Category> categories = categoryRepository.findCategoryByUserAndDeletedFalse(user);
//
//        return categories.stream()
//                .map(categoryMapper::toReadOnly)
//                .toList();
//    }


    @Transactional(readOnly = true)
    @Override
    public Page<CategoryReadOnlyDTO> getFilteredPaginatedCategories(UUID userUuid, CategoryFilters filters, Pageable pageable) {
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + userUuid + " not found"));

        var specification = CategorySpecification.build(filters, user);

        return categoryRepository.findAll(specification, pageable)
                .map(categoryMapper::toReadOnly);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidArgumentException("Category", "Category name is required");
        }
        return name.trim();
    }
}
