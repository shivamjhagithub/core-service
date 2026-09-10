package com.CoreService.CoreService.dashboard.dto;

import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.AttendanceSummary;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.SubjectAttendance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttendanceSummaryTest {

    @Test
    void subjectBreakdownRollsIntoOverallAndCountsLateAsPresent() {
        UUID mathsId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SubjectAttendance maths = SubjectAttendance.of(mathsId, "Mathematics", 8, 1, 1, 0);
        SubjectAttendance untitled = SubjectAttendance.of(null, null, 2, 0, 2, 0);

        AttendanceSummary summary = AttendanceSummary.of(10, 1, 3, 0, List.of(maths, untitled));

        assertEquals(10, summary.presentCount());
        assertEquals(1, summary.lateCount());
        assertEquals(3, summary.absentCount());
        assertEquals(14, summary.totalCount());
        assertEquals(78.57, summary.attendancePercentage());
        assertEquals(2, summary.bySubject().size());
        assertEquals(90.0, maths.attendancePercentage());
        assertNull(untitled.subjectId());
    }
}
