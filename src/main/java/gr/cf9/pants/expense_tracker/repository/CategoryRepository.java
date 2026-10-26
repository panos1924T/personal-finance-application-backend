package gr.cf9.pants.expense_tracker.repository;

import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.Transaction;
import gr.cf9.pants.expense_tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    Optional<Category> findCategoryByUuidAndUserAndDeletedFalse(UUID uuid, User user);

    Optional<Category> findCategoryByUuidAndUser(UUID Uuid, User user);

    Optional<Category> findCategoryByUuidAndUserAndTypeAndDeletedFalse(
            UUID uuid, User user, TransactionType transactionType
    );

    boolean existsCategoryByUserAndNameAndTypeAndParentIsNull(
            User user,
            String name,
            TransactionType type
    );

    boolean existsCategoryByUserAndNameAndTypeAndParent(
            User user,
            String name,
            TransactionType type,
            Category parent
    );

    boolean existsCategoryByParentAndDeletedFalse(Category parent);

    boolean existsCategoryByUserAndNameAndTypeAndParentIsNullAndUuidNot(User user, String name, TransactionType type, UUID uuid);

    boolean existsCategoryByUserAndNameAndTypeAndParentAndUuidNot(User user, String name, TransactionType type, Category parent, UUID uuid);

    Page<Category> findAll(@Nullable Specification<Category> specification, Pageable pageable);
}
