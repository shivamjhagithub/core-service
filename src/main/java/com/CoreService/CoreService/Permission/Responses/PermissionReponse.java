package com.CoreService.CoreService.Permission.Responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PermissionReponse {
    String permissionName;
    String permissionDescription;
    String permissionCode;
    String permissionId;
}
