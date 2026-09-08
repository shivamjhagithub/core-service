package com.CoreService.CoreService.common.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public class JwtDto {
    String userId;
    UUID collegeId;
    List<String> roles;
    List<String> permissions;
    List<String> modules;


}
