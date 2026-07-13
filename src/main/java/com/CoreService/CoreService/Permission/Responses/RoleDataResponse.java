package com.CoreService.CoreService.Permission.Responses;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class RoleDataResponse {
     UUID roleId;
     String roleName;
     String roleDescription;
}
