package com.saeal.MrDaebackService.menuItems.repository;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuItemsRepository extends JpaRepository<MenuItems, UUID> {
}
