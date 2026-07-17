package edu.training.library;

import com.formdev.flatlaf.FlatLightLaf;
import edu.training.library.db.Database;
import edu.training.library.db.LibraryRepository;
import edu.training.library.service.LibraryService;
import edu.training.library.ui.LoginFrame;
import edu.training.library.ui.Ui;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public final class LibraryApplication {
    private LibraryApplication() {}

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        FlatLightLaf.setup();
        Ui.installDefaults();
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        Database database = Database.fromEnvironment();
                        database.initialize();
                        LibraryService service =
                                new LibraryService(new LibraryRepository(database));
                        service.seedDemoData();
                        service.reconcileReservationInventory();
                        startReservationExpiryTask(service);
                        new LoginFrame(service).setVisible(true);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null, e.getMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
                    }
                });
    }

    private static void startReservationExpiryTask(LibraryService service) {
        var executor =
                Executors.newSingleThreadScheduledExecutor(
                        task -> {
                            Thread thread = new Thread(task, "reservation-expiry");
                            thread.setDaemon(true);
                            return thread;
                        });
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        service.expireReservations();
                    } catch (RuntimeException e) {
                        System.err.println("预约过期处理失败：" + e.getMessage());
                    }
                },
                1,
                1,
                TimeUnit.MINUTES);
    }
}
