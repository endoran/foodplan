package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.ScanSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScanSessionRepository extends MongoRepository<ScanSession, String> {
}
