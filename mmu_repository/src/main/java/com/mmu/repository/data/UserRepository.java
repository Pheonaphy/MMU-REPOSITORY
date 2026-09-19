package com.mmu.repository.data;

import com.mmu.repository.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Looks up a user record in the PostgreSQL database based on their username.
     * This is essential for the custom authentication login steps and registration safety checks.
     * * @param username The unique username string to search for (case sensitive)
     * @return The complete User entity object if found, or null if it doesn't exist
     */
    User findByUsername(String username);
}
