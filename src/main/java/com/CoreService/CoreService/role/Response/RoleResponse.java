package com.CoreService.CoreService.role.Response;

import com.CoreService.CoreService.common.Response.BasicResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@Builder
@NoArgsConstructor
public class RoleResponse  extends BasicResponse {
    private UUID roleId;
    private String roleName;
    private String roleDescription;
}
