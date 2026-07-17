package edu.training.library.ui;

import edu.training.library.model.Models.Role;
import java.util.ArrayList;
import java.util.List;

final class LibraryNavigation {
    private LibraryNavigation() {}

    static List<Item> forRole(Role role) {
        List<Item> items = new ArrayList<>();
        items.add(new Item("dashboard", "01", "总览"));
        items.add(new Item("books", "02", role == Role.ADMIN ? "图书管理" : "馆藏检索"));
        if (role == Role.ADMIN) items.add(new Item("users", "03", "读者管理"));
        items.add(new Item("loans", "04", role == Role.ADMIN ? "借还管理" : "我的借阅"));
        items.add(new Item("reservations", "05", "预约管理"));
        items.add(new Item("fines", "06", "罚款记录"));
        items.add(new Item("statistics", "07", "统计分析"));
        return List.copyOf(items);
    }

    record Item(String id, String glyph, String label) {}
}
