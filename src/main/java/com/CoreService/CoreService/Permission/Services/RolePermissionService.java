package com.CoreService.CoreService.Permission.Services;

import com.CoreService.CoreService.Permission.Entities.PermissionEntity;
import com.CoreService.CoreService.Permission.Entities.RolePermissionEntity;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.Permission.Responses.PermissionReponse;
import com.CoreService.CoreService.Permission.Responses.RoleDataResponse;
import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor

@RequiredArgsConstructor
public class RolePermissionService {
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CollegeContext collegeContext;
    public boolean addPermissionToRole(UUID roleId, String permissionId) {
        try {
            if(rolePermissionRepository.existsByRole_roleIdAndPermission_permissionId(roleId, permissionId)) {
                return false;
            }
            Role role = roleRepository.findByRoleIdAndCollege_collegeId(roleId,collegeContext.getCollegeId())
                    .orElseThrow(() -> new RuntimeException("Role not found."));

            PermissionEntity permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new RuntimeException("Permission not found."));

            RolePermissionEntity rolePermissionEntity = RolePermissionEntity.builder()
                    .role(role)
                    .permission(permission)
                    .build();
            rolePermissionRepository.save(rolePermissionEntity);
            return true;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<RoleDataResponse>  getAllRolesForPermission(String permissionId) {


        if(collegeContext.getCollegeId()!=null) {
            UUID collegeId = collegeContext.getCollegeId();
        List<RolePermissionEntity> rolePermissionEntities=rolePermissionRepository.findAllByPermission_permissionIdAndRole_College_collegeId(permissionId,collegeId);
        List<RoleDataResponse> roleResponses=new ArrayList<>();
        for (RolePermissionEntity rolePermissionEntity : rolePermissionEntities) {
            roleResponses.add(mapToRoleResponse(rolePermissionEntity));
        }
        return roleResponses;}
        return new ArrayList<>();
    }
    public List<PermissionReponse> getAllPermissionsForRole(UUID roleId) {
        List<RolePermissionEntity> rolePermissionEntities=rolePermissionRepository.findAllByRole_roleId(roleId);
        List<PermissionReponse> permissionResponses=new ArrayList<>();
        for (RolePermissionEntity rolePermissionEntity : rolePermissionEntities) {
            permissionResponses.add(mapToPermissionResponse(rolePermissionEntity));
        }
        return permissionResponses;
    }
    public void deletePermissionFromRole(UUID roleId, String permissionId) {
        rolePermissionRepository.deleteByRole_roleIdAndPermission_permissionId(roleId, permissionId);
    }
    public void deleteAllPermissionsForRole(UUID roleId) {
        rolePermissionRepository.deleteAllByRole_roleId(roleId);
    }
    public boolean isExistsByRoleIdAndPermission_permissionId(UUID roleId, String permissionId) {
        return rolePermissionRepository.existsByRole_roleIdAndPermission_permissionId(roleId, permissionId);
    }
    public RoleDataResponse mapToRoleResponse(RolePermissionEntity rolePermissionEntity) {
        Role role=rolePermissionEntity.getRole();
        return RoleDataResponse.builder().roleDescription(role.getRoleDescription()).roleId(role.getRoleId()).roleName(role.getRoleName()).build();
    }
    public PermissionReponse mapToPermissionResponse(RolePermissionEntity rolePermissionEntity) {
        PermissionEntity permission=rolePermissionEntity.getPermission();
        return PermissionReponse.builder().permissionCode(permission.getPermissionCode()).permissionName(permission.getPermissionName()).permissionDescription(permission.getPermissionDescription()).permissionId(permission.getPermissionId()).build();
    }
    public void updateRolePermissions(UUID roleId, List<String> permissionIds) {

        Role role = roleRepository
                .findByRoleIdAndCollege_collegeId(
                        roleId,
                        collegeContext.getCollegeId())
                .orElseThrow(() -> new RuntimeException("Role not found."));

        rolePermissionRepository.deleteAllByRole_roleId(roleId);

        List<RolePermissionEntity> rolePermissions = new ArrayList<>();

        for (String permissionId : permissionIds) {

            PermissionEntity permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new RuntimeException("Permission not found."));

            rolePermissions.add(
                    RolePermissionEntity.builder()
                            .role(role)
                            .permission(permission)
                            .build()
            );
        }

        rolePermissionRepository.saveAll(rolePermissions);
    }
}
