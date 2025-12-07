package com.saeal.MrDaebackService.user.repository;

import com.saeal.MrDaebackService.user.domain.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCardRepository extends JpaRepository<UserCard, UUID> {
    List<UserCard> findByUserId(UUID userId);
}
