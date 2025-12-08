package com.saeal.MrDaebackService.menuItems.repository;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemsRepository extends JpaRepository<MenuItems, UUID> {
    /**
     * 이름으로 메뉴 아이템 찾기 (대소문자 무시) - 중복 가능
     */
    List<MenuItems> findByNameIgnoreCase(String name);

    /**
     * 이름에 포함된 메뉴 아이템 목록 찾기 (대소문자 무시)
     */
    List<MenuItems> findByNameContainingIgnoreCase(String name);
}
