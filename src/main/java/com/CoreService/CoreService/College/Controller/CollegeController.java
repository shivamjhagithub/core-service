package com.CoreService.CoreService.College.Controller;

import com.CoreService.CoreService.College.Dto.CollegeDto;
import com.CoreService.CoreService.College.Dto.CollegeProvisioningResult;
import com.CoreService.CoreService.College.Request.CollegeDataRequest;
import com.CoreService.CoreService.College.Services.CollegeService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.TenantGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/college")
@RequiredArgsConstructor
@Tag(name = "College", description = "Tenant registration and settings")
public class CollegeController {

    private final CollegeService collegeService;
    private final TenantGuard tenantGuard;

    @PostMapping({"", "/", "/createCollege"})
    @PreAuthorize("hasRole('MAIN_ADMIN')")
    @Operation(summary = "Provision a college",
            description = """
                    Creates the tenant, its default roles, its first administrator and its
                    enabled modules. The administrator's temporary password is returned once
                    and cannot be retrieved again.""")
    public ResponseEntity<ApiResponse<CollegeProvisioningResult>> createCollege(
            @Valid @RequestBody CollegeDataRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("College created successfully",
                        collegeService.createCollege(request)));
    }

    @GetMapping("/allCollege")
    @PreAuthorize("hasRole('MAIN_ADMIN')")
    @Operation(summary = "List every college on the platform")
    public ResponseEntity<ApiResponse<PageResponse<CollegeDto>>> getAllCollege(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(collegeService.getAllCollege(page, size))));
    }

    @GetMapping("/myCollege")
    @Operation(summary = "Read the college of the authenticated user")
    public ResponseEntity<ApiResponse<CollegeDto>> getMyCollege() {
        return ResponseEntity.ok(ApiResponse.success(
                collegeService.getCollegeByCollegeId(tenantGuard.requireCollegeId())));
    }

    @PutMapping({"", "/"})
    @PreAuthorize("hasAuthority('UPDATE_SETTINGS')")
    @Operation(summary = "Update the college of the authenticated user")
    public ResponseEntity<ApiResponse<CollegeDto>> updateCollegeData(
            @Valid @RequestBody CollegeDataRequest request) {

        return ResponseEntity.ok(ApiResponse.success("College updated",
                collegeService.updateCollegeData(tenantGuard.requireCollegeId(), request)));
    }

    @DeleteMapping("/{collegeId}")
    @PreAuthorize("hasRole('MAIN_ADMIN')")
    @Operation(summary = "Delete a college and everything it owns")
    public ResponseEntity<ApiResponse<Void>> deleteCollege(@PathVariable UUID collegeId) {
        collegeService.deleteCollegeData(collegeId);
        return ResponseEntity.ok(ApiResponse.message("College deleted successfully"));
    }

    @GetMapping("/{collegeId}")
    @PreAuthorize("hasRole('MAIN_ADMIN')")
    @Operation(summary = "Read any college by id")
    public ResponseEntity<ApiResponse<CollegeDto>> getCollegeById(@PathVariable UUID collegeId) {
        return ResponseEntity.ok(ApiResponse.success(collegeService.getCollegeByCollegeId(collegeId)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('MAIN_ADMIN')")
    @Operation(summary = "Search colleges by name, university or city")
    public ResponseEntity<ApiResponse<PageResponse<CollegeDto>>> searchColleges(
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false) String universityName,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Colleges fetched successfully",
                PageResponse.from(collegeService.searchColleges(collegeName, universityName, city, page, size))));
    }
}
