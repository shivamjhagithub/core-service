package com.CoreService.CoreService.role.Services;

import com.CoreService.CoreService.role.Requests.RoleRequest;
import com.CoreService.CoreService.role.Response.RoleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RoleService {

    RoleResponse createRole(RoleRequest request);

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID roleId);

    RoleResponse updateRole(UUID roleId, RoleRequest request);

    void deleteRole(UUID roleId);
}
