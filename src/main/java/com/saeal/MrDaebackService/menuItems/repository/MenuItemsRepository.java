package com.saeal.MrDaebackService.menuItems.repository;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenuItemsRepository extends JpaRepository<MenuItems, UUID> {
    /**
     * 이름으로 메뉴 아이템 찾기 (대소문자 무시)
     */
    Optional<MenuItems> findByNameIgnoreCase(String name);

    /**
     * 이름에 포함된 메뉴 아이템 찾기 (대소문자 무시)
     */
    Optional<MenuItems> findFirstByNameContainingIgnoreCase(String name);
}
