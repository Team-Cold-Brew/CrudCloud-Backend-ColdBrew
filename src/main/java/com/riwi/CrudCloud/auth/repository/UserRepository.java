package com.riwi.CrudCloud.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.models.UserType;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find user by email
     *
     * @param email the user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     *
     * @param username the user's username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find all active users of a specific type
     *
     * @param userType the type of user (INDIVIDUAL or ORGANIZATIONAL_USER)
     * @return List of active users of the specified type
     */
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.deletedAt IS NULL")
    List<User> findActiveUsersByType(@Param("userType") UserType userType);

    /**
     * Find all users with a specific personal plan (for individuals)
     *
     * @param planId the plan ID
     * @return List of users assigned to this plan
     */
    @Query("SELECT u FROM User u WHERE u.personalPlan.planId = :planId AND u.deletedAt IS NULL")
    List<User> findUsersByPersonalPlanId(@Param("planId") Integer planId);

    /**
     * Check if email already exists (useful for registration validation)
     *
     * @param email the email to check
     * @return true if email exists and not deleted
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    /**
     * Check if username already exists
     *
     * @param username the username to check
     * @return true if username exists and not deleted
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);

    /**
     * Find user by Google ID
     *
     * @param googleId the Google ID
     * @return Optional containing the user if found
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Find user by GitHub ID
     *
     * @param githubId the GitHub ID
     * @return Optional containing the user if found
     */
    Optional<User> findByGithubId(String githubId);

    /**
     * Find user by email and not deleted
     *
     * @param email the user's email
     * @return Optional containing the user if found and not deleted
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    /**
     * Find user by Google ID and not deleted
     *
     * @param googleId the Google OAuth ID
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE u.googleId = :googleId AND u.deletedAt IS NULL")
    Optional<User> findByGoogleIdAndNotDeleted(@Param("googleId") String googleId);

    /**
     * Find user by GitHub ID and not deleted
     *
     * @param githubId the GitHub OAuth ID
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE u.githubId = :githubId AND u.deletedAt IS NULL")
    Optional<User> findByGithubIdAndNotDeleted(@Param("githubId") String githubId);
}
