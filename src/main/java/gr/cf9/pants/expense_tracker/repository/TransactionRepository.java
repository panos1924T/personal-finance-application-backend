package gr.cf9.pants.expense_tracker.repository;

import gr.cf9.pants.expense_tracker.model.Transaction;
import gr.cf9.pants.expense_tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findTransByUuidAndUser(UUID uuid, User user);

    Page<Transaction> findAll(@Nullable Specification<Transaction> specification, Pageable pageable);
}
