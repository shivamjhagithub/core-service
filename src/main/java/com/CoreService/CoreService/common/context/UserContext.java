package com.CoreService.CoreService.common.context;

import lombok.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class UserContext {
    String userId;
    UUID collegeId;
}
