package edu.training.library.ui;

import edu.training.library.model.Models.Role;
import java.util.ArrayList;
import java.util.List;

final class LibraryNavigation {
    private LibraryNavigation() {}

    static List<Item> forRole(Role role) {
        List<String[]> entries = new ArrayList<>();
        entries.add(new String[] {"dashboard", "总览"});
        entries.add(new String[] {"books", role == Role.ADMIN ? "图书管理" : "馆藏检索"});
        if (role == Role.ADMIN) entries.add(new String[] {"users", "读者管理"});
        entries.add(new String[] {"loans", role == Role.ADMIN ? "借还管理" : "我的借阅"});
        entries.add(new String[] {"reservations", "预约管理"});
        entries.add(new String[] {"fines", "罚款记录"});
        entries.add(new String[] {"statistics", "统计分析"});
        List<Item> items = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            String[] entry = entries.get(index);
            items.add(new Item(entry[0], String.format("%02d", index + 1), entry[1]));
        }
        return List.copyOf(items);
    }

    record Item(String id, String glyph, String label) {}
}
