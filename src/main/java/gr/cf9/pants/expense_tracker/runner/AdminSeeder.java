package gr.cf9.pants.expense_tracker.runner;

import gr.cf9.pants.expense_tracker.model.Role;
import gr.cf9.pants.expense_tracker.model.User;
import gr.cf9.pants.expense_tracker.repository.RoleRepository;
import gr.cf9.pants.expense_tracker.repository.UserRepository;
import gr.cf9.pants.expense_tracker.service.UserDefaultsInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDefaultsInitializer defaultsInitializer;

    @Value("${spring.admin.email:#{null}}")
    private String adminEmail;

    @Value("${spring.admin.password:#{null}}")
    private String adminPassword;

    @Transactional
    @Override
    public void run(String... args) throws Exception {

        if (adminEmail == null || adminPassword == null) {
            System.out.println("No admin credentials provided via environment variables. Skipping admin seeder.");
            return;
        }

        if (userRepository.findUserByEmail(adminEmail).isEmpty()) {

            Optional<Role> roleOpt = roleRepository.findRoleByName("ADMIN");
            if (roleOpt.isEmpty()) {
                System.err.println("[Seeder Error] ADMIN role not found. Admin creation aborted.");
                return;
            }

            Role adminRole = roleOpt.get();

            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setDisplayName("superadmin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(adminRole);

            userRepository.save(admin);
            defaultsInitializer.createDefaultCategories(admin);
            defaultsInitializer.createDefaultCashAccount(admin);
            System.out.println("Superadmin created successfully with email= " + adminEmail);
        }
    }
}