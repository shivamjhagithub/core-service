package com.CoreService.CoreService.role.Controller;

import com.CoreService.CoreService.role.Requests.RoleRequest;
import com.CoreService.CoreService.role.Response.RoleResponse;
import com.CoreService.CoreService.role.Services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    @Autowired
    private final RoleService roleService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody RoleRequest request) {

        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/getOne/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(
            @PathVariable UUID roleId) {

        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @PutMapping("/update/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleRequest request) {

        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/delete/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(
            @PathVariable UUID roleId) {

        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }
}
