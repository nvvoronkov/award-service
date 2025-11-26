package org.example.repository;

import org.example.model.Award;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AwardRepository extends ReactiveCrudRepository<Award, Long> {
}
