package com.saeal.MrDaebackService.menuItems;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuItemsRepository extends JpaRepository<MenuItems, UUID> {
}
