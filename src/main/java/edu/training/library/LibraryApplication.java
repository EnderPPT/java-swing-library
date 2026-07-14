package edu.training.library;

import com.formdev.flatlaf.FlatLightLaf;
import edu.training.library.db.Database;
import edu.training.library.db.LibraryRepository;
import edu.training.library.service.LibraryService;
import edu.training.library.ui.LoginFrame;
import java.awt.*;
import javax.swing.*;

public final class LibraryApplication {
    private LibraryApplication() {}

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("defaultFont", new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        Database database = Database.fromEnvironment();
                        database.initialize();
                        LibraryService service =
                                new LibraryService(new LibraryRepository(database));
                        service.seedDemoData();
                        new LoginFrame(service).setVisible(true);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null, e.getMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
                    }
                });
    }
}
