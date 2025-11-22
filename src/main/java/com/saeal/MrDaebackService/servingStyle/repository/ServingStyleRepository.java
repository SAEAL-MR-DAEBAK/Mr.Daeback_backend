package com.saeal.MrDaebackService.servingStyle.repository;

import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServingStyleRepository extends JpaRepository<ServingStyle, UUID> {
}
