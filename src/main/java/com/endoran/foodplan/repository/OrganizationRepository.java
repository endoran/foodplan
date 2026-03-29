package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    List<Organization> findByNameContainingIgnoreCase(String name);
}
