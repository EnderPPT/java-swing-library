package edu.training.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class Models {
    private Models() {}

    public enum Role {
        ADMIN,
        READER
    }

    public record User(
            long id,
            String username,
            String fullName,
            String phone,
            String email,
            Role role,
            String cardStatus) {
        @Override
        public String toString() {
            return fullName + " (" + username + ")";
        }
    }

    public record Book(
            long id,
            String isbn,
            String title,
            String author,
            String publisher,
            String category,
            int totalCopies,
            int availableCopies,
            String location) {}

    public record Loan(
            long id,
            long userId,
            String readerName,
            long bookId,
            String bookTitle,
            LocalDateTime borrowedAt,
            LocalDateTime dueAt,
            LocalDateTime returnedAt,
            int renewCount,
            String status) {}

    public record Reservation(
            long id,
            long userId,
            String readerName,
            long bookId,
            String bookTitle,
            LocalDateTime reservedAt,
            LocalDateTime expiresAt,
            String status,
            boolean notified) {}

    public record Fine(
            long id,
            long loanId,
            long userId,
            String readerName,
            String bookTitle,
            BigDecimal amount,
            String reason,
            String status,
            LocalDateTime paidAt) {}

    public record Dashboard(
            long bookKinds,
            long totalCopies,
            long availableCopies,
            long activeLoans,
            long waitingReservations,
            BigDecimal unpaidFines) {}

    public record Ranking(String title, String author, long borrowCount) {}

    public record MonthlyStat(String month, long borrowCount) {}

    public record CategoryStock(
            String category, long bookKinds, long totalCopies, long availableCopies) {}
}
