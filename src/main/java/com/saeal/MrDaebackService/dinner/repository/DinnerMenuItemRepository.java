package com.saeal.MrDaebackService.dinner.repository;

import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DinnerMenuItemRepository extends JpaRepository<DinnerMenuItem, UUID> {
    List<DinnerMenuItem> findByDinnerId(UUID dinnerId);
}
