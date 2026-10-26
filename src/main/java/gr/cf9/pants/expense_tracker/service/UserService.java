package gr.cf9.pants.expense_tracker.service;

import gr.cf9.pants.expense_tracker.core.enums.AccountType;
import gr.cf9.pants.expense_tracker.core.enums.TransactionType;
import gr.cf9.pants.expense_tracker.core.exceptions.EntityAlreadyExistsException;
import gr.cf9.pants.expense_tracker.core.exceptions.EntityNotFoundException;
import gr.cf9.pants.expense_tracker.dto.user_dto.UserReadOnlyDTO;
import gr.cf9.pants.expense_tracker.dto.user_dto.UserInsertDTO;
import gr.cf9.pants.expense_tracker.dto.user_dto.UserUpdateDTO;
import gr.cf9.pants.expense_tracker.mapper.UserMapper;
import gr.cf9.pants.expense_tracker.model.Account;
import gr.cf9.pants.expense_tracker.model.Category;
import gr.cf9.pants.expense_tracker.model.Role;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.AccountRepository;
import gr.cf9.pants.expense_tracker.repository.CategoryRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    private record ParentCategorySeed(String name, TransactionType type) {}

    private static final List<ParentCategorySeed> DEFAULT_CATEGORIES = List.of(
            new ParentCategorySeed("Personal",       TransactionType.EXPENSE),
            new ParentCategorySeed("Home",           TransactionType.EXPENSE),
            new ParentCategorySeed("Buys",           TransactionType.EXPENSE),
            new ParentCategorySeed("Transportation", TransactionType.EXPENSE),
            new ParentCategorySeed("Bills",          TransactionType.EXPENSE),
            new ParentCategorySeed("Other",          TransactionType.EXPENSE),

            new ParentCategorySeed("Salary",         TransactionType.INCOME),
            new ParentCategorySeed("Business",       TransactionType.INCOME),
            new ParentCategorySeed("Investment",     TransactionType.INCOME)
    );

    @Transactional
    @Override
    public UserReadOnlyDTO saveUser(UserInsertDTO userInsertDTO) {

        //VALIDATE
        if (userRepository.existsUserByEmail(userInsertDTO.email())) {
            throw new EntityAlreadyExistsException("User", "Email already exists: " + userInsertDTO.email());
        }

        //PREPARE
        Role citizenRole = new Role();
        citizenRole.setId(2L);
        String hashedPassword = passwordEncoder.encode(userInsertDTO.password());
        User user = userMapper.toEntity(userInsertDTO, hashedPassword);
        user.setRole(citizenRole);

        //EXECUTE
        User savedUser = userRepository.save(user);
        createDefaultCategories(savedUser);
        createDefaultCashAccount(savedUser);

        //RETURN
        return userMapper.toReadOnly(savedUser);
    }

    @PreAuthorize("hasAuthority('EDIT_CITIZEN') or " +
            "(hasAuthority('EDIT_ONLY_CITIZEN') and #userUuid == authentication.principal.uuid)")
    @Transactional
    @Override
    public UserReadOnlyDTO updateUser(UUID userUuid, UserUpdateDTO userUpdateDTO) {

        //VALIDATE
        User user = userRepository.findUserByUuidAndDeletedFalse(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User not found with uuid: " + userUuid));

        if (userRepository.existsUserByEmailAndUuidNot(userUpdateDTO.email(), userUuid)) {
            throw new EntityAlreadyExistsException("User", "Email already exists! " + userUpdateDTO.email());
        }

        //PREPARE
        user.setDisplayName(userUpdateDTO.displayName());
        user.setEmail(userUpdateDTO.email());
        user.setPassword(passwordEncoder.encode(userUpdateDTO.password()));

        //EXECUTE
        User updatedUser = userRepository.save(user);

        //RETURN
        return userMapper.toReadOnly(updatedUser);
    }

    @PreAuthorize("hasAuthority('DEACTIVATE_CITIZEN') or " +
            "(hasAuthority('DEACTIVATE_ONLY_CITIZEN') and #uuid == authentication.principal.uuid)")
    @Transactional
    @Override
    public UserReadOnlyDTO deleteUser(UUID uuid) {
        User user = userRepository.findUserByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + uuid + " not found!"));

        user.softDelete(Instant.now());
        userRepository.save(user);

        return userMapper.toReadOnly(user);
    }

    @PreAuthorize("hasAuthority('VIEW_CITIZEN')")
    @Override
    @Transactional(readOnly = true)
    public UserReadOnlyDTO getUserByUuid(UUID uuid) {
        User user = userRepository.findUserByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + uuid + " not found!"));
        return userMapper.toReadOnly(user);
    }

    @PreAuthorize("hasAuthority('VIEW_CITIZEN') or " +
            "(hasAuthority('VIEW_ONLY_CITIZEN') and #uuid == authentication.principal.uuid)")
    @Override
    @Transactional(readOnly = true)
    public UserReadOnlyDTO getUserByUuidAndDeletedFalse(UUID uuid) {
        User user = userRepository.findUserByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid: " + uuid + " not found!"));
        return userMapper.toReadOnly(user);
    }

    @PreAuthorize("hasAuthority('VIEW_CITIZENS')")
    @Transactional(readOnly = true)
    @Override
    public Page<UserReadOnlyDTO> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        log.debug("Get paginated returned successfully page={} and size={}", usersPage.getNumber(), usersPage.getSize());

        return usersPage.map(userMapper::toReadOnly);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VIEW_CITIZENS')")
    @Override
    public Page<UserReadOnlyDTO> getAllUsersDeletedFalse(Pageable pageable) {
        Page<User> usersPage = userRepository.findAllUsersByDeletedFalse(pageable);
        log.debug("Get paginated returned successfully page={} and size={}", usersPage.getNumber(), usersPage.getSize());

        return usersPage.map(userMapper::toReadOnly);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserExists(String email) {
        return userRepository.existsUserByEmail(email);
    }

    private void createDefaultCategories(User user) {
        List<Category> categories = DEFAULT_CATEGORIES.stream()
                .map(seed -> {
                    Category c = new Category();
                    c.setUser(user);
                    c.setName(seed.name());
                    c.setType(seed.type());
                    return c;
                })
                .toList();

        categoryRepository.saveAll(categories);
    }

    private void createDefaultCashAccount(User user) {
        Account cashDefault = new Account(
                "Cash",
                BigDecimal.ZERO,
                AccountType.LIQUIDITY,
                true,
                user
        );

        accountRepository.save(cashDefault);
    }
}
