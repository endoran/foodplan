package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.TrainingPair;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TrainingPairRepository extends MongoRepository<TrainingPair, String> {
    List<TrainingPair> findByHasCorrectionsTrue();
}
