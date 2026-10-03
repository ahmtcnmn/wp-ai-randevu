package com.appointflow.ai.handler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeParseUtil {

    private DateTimeParseUtil() {}

    public static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim().toLowerCase();
        if (trimmed.contains("bugün") || trimmed.contains("bugun")) return LocalDate.now();
        if (trimmed.contains("yarın") || trimmed.contains("yarin")) return LocalDate.now().plusDays(1);
        if (trimmed.contains("öbür gün") || trimmed.contains("obur gun")) return LocalDate.now().plusDays(2);

        // ISO
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException ignored) {}
        // dd.MM.yyyy
        try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy")); } catch (DateTimeParseException ignored) {}
        // dd/MM/yyyy
        try { return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (DateTimeParseException ignored) {}
        return null;
    }

    public static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        String norm = s.trim().replace('.', ':');
        // 14:30 or 14:30:00
        try { return LocalTime.parse(norm); } catch (DateTimeParseException ignored) {}
        // 14 -> 14:00
        try {
            int h = Integer.parseInt(norm);
            if (h >= 0 && h <= 23) return LocalTime.of(h, 0);
        } catch (NumberFormatException ignored) {}
        // HH:mm
        try { return LocalTime.parse(norm, DateTimeFormatter.ofPattern("H:mm")); } catch (DateTimeParseException ignored) {}
        return null;
    }

    public static LocalDateTime combine(LocalDate date, LocalTime time) {
        if (date == null || time == null) return null;
        return LocalDateTime.of(date, time);
    }
}
