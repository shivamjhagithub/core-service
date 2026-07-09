package com.CoreService.CoreService.role.Services;

import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import com.CoreService.CoreService.role.Requests.RoleRequest;
import com.CoreService.CoreService.role.Response.RoleResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

        @Override
        public RoleResponse createRole(RoleRequest request) {

            if (request.getRoleName() == null || request.getRoleName().trim().isEmpty()) {
                throw new RuntimeException("Role name cannot be empty.");
            }

            if (roleRepository.existsByName(request.getRoleName())) {
                throw new RuntimeException("Role already exists.");
            }

            Role role = new Role();

            role.setRoleName(request.getRoleName());
            role.setRoleDescription(request.getRoleDescription());

            Role savedRole = roleRepository.save(role);

            return mapToResponse(savedRole);
        }

        @Override
        public List<RoleResponse> getAllRoles() {

            return roleRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        @Override
        public RoleResponse getRoleById(UUID roleId) {

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found."));

            return mapToResponse(role);
        }

        @Override
        public RoleResponse updateRole(UUID roleId, @org.jetbrains.annotations.UnknownNullability RoleRequest request) {

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found."));

            if (request.getRoleName() == null || request.getRoleName().trim().isEmpty()) {
                throw new RuntimeException("Role name cannot be empty.");
            }

            role.setRoleName(request.getRoleName());
            role.setRoleDescription(request.getRoleDescription());

            Role updatedRole = roleRepository.save(role);

            return mapToResponse(updatedRole);
        }

        @Override
        public void deleteRole(UUID roleId) {

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found."));

            roleRepository.delete(role);
        }

        private RoleResponse mapToResponse(Role role) {

            RoleResponse response = new RoleResponse();

            response.setRoleId(role.getRoleId());
            response.setRoleName(role.getRoleName());
            response.setRoleDescription(role.getRoleDescription());

            return response;
        }
}
