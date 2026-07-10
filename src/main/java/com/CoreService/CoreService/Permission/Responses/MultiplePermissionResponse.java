package com.CoreService.CoreService.Permission.Responses;

import com.CoreService.CoreService.common.Response.BasicResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@Setter
public class MultiplePermissionResponse extends BasicResponse {
    List<PermissionReponse> permssions;
}
