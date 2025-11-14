package com.riwi.CrudCloud.auth.dto.response;

import java.time.LocalDateTime;

import com.riwi.CrudCloud.common.models.UserStatus;
import com.riwi.CrudCloud.common.models.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Integer userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private UserType userType;
    private UserStatus status;
    private LocalDateTime createdAt;
}
