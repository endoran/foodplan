package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByOrgId(String orgId);

    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
