package gr.cf9.pants.expense_tracker.repository;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.dto.account_dto.AccountReadOnlyDTO;
import gr.cf9.pants.expense_tracker.model.Account;
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
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    Optional<Account> findAccountByUuidAndUser(UUID uuid, User user);

    Optional<Account> findAccountByUuidAndUserAndDeletedFalse(UUID uuid, User user);

    Optional<Account> findAccountByUserAndDefaultAccountTrue(User user);

    boolean existsAccountByUserAndNameAndDeletedFalse(User user, String name);

    boolean existsAccountByUserAndNameAndUuidNotAndDeletedFalse(User user, String name, UUID uuid);

    boolean existsAccountByUserAndDefaultAccountTrue(User user);

    boolean existsAccountByUuidAndUserAndDeletedFalse(UUID uuid, User user);

    Page<Account> findAll(@Nullable Specification<Account> specification, Pageable pageable);
}
