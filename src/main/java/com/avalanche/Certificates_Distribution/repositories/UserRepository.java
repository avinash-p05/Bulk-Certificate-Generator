package com.avalanche.Certificates_Distribution.repositories;


import com.avalanche.Certificates_Distribution.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// UserRepository.java
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Find all users where email hasn't been sent yet
    List<User> findByEmailSentFalse();

    // Find users by email
    Optional<User> findByEmail(String email);

    // Find users by USN
    Optional<User> findByUsn(String usn);

    // Find users with error messages
    List<User> findByErrorMessageNotNull();

    // Find users where email was sent successfully
    List<User> findByEmailSentTrue();

    // Count users where email hasn't been sent
    long countByEmailSentFalse();

    // Count users where email was sent successfully
    long countByEmailSentTrue();

    // Find users by email sent status and error message existence
    List<User> findByEmailSentAndErrorMessageNotNull(boolean emailSent);

    // Custom query to find users by email domain
    @Query("{'email': {$regex: ?0}}")
    List<User> findByEmailDomain(String domain);

    // Delete users by email domain
    void deleteByEmailContaining(String domain);

    // Update error message for a specific user
    @Query("{'_id': ?0}")
    @Update("{'$set': {'errorMessage': ?1}}")
    void updateErrorMessage(String id, String errorMessage);

    // Update email sent status for a specific user
    @Query("{'_id': ?0}")
    @Update("{'$set': {'emailSent': ?1}}")
    void updateEmailSentStatus(String id, boolean status);

    // Bulk update email sent status
    @Query("{'_id': {'$in': ?0}}")
    @Update("{'$set': {'emailSent': ?1}}")
    void bulkUpdateEmailSentStatus(List<String> ids, boolean status);
}
