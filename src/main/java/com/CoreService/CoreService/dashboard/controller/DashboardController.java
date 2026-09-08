package com.CoreService.CoreService.dashboard.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.dashboard.dto.CollegeAdminDashboardResponse;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse;
import com.CoreService.CoreService.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/student")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<StudentDashboardResponse> student() {
        return ApiResponse.success(dashboardService.studentDashboard());
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<TeacherDashboardResponse> teacher() {
        return ApiResponse.success(dashboardService.teacherDashboard());
    }

    @GetMapping("/college-admin")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') and hasRole('COLLEGE_ADMIN')")
    public ApiResponse<CollegeAdminDashboardResponse> collegeAdmin() {
        return ApiResponse.success(dashboardService.collegeAdminDashboard());
    }
}
