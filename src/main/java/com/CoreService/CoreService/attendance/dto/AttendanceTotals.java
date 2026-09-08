package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

import java.util.Map;

public record AttendanceTotals(long total,
                               long present,
                               long absent,
                               long late,
                               long excused,
                               double attendancePercentage) {

    public static final AttendanceTotals EMPTY = new AttendanceTotals(0L, 0L, 0L, 0L, 0L, 0.0);

    /**
     * PRESENT and LATE count as attended; the percentage is rounded to two
     * decimals and is zero while nothing has been marked.
     */
    public static AttendanceTotals of(Map<AttendanceStatus, Long> countsByStatus) {
        long present = count(countsByStatus, AttendanceStatus.PRESENT);
        long absent = count(countsByStatus, AttendanceStatus.ABSENT);
        long late = count(countsByStatus, AttendanceStatus.LATE);
        long excused = count(countsByStatus, AttendanceStatus.EXCUSED);
        long total = present + absent + late + excused;

        if (total == 0L) {
            return EMPTY;
        }

        double percentage = Math.round((present + late) * 10000.0 / total) / 100.0;
        return new AttendanceTotals(total, present, absent, late, excused, percentage);
    }

    private static long count(Map<AttendanceStatus, Long> countsByStatus, AttendanceStatus status) {
        Long value = countsByStatus.get(status);
        return value == null ? 0L : value;
    }
}
