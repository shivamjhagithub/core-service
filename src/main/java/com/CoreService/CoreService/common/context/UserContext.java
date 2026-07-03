package com.CoreService.CoreService.common.context;

import lombok.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class UserContext {
    String userId;
    String collegeId;
}
