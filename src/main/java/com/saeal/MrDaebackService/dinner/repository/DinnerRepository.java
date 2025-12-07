package com.saeal.MrDaebackService.dinner.repository;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DinnerRepository extends JpaRepository<Dinner, UUID> {
}
