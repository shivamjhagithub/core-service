package com.CoreService.CoreService.user.Services;

import com.CoreService.CoreService.user.Requests.UserRequsets;
import com.CoreService.CoreService.user.Responses.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(UserRequsets requsets);
    UserResponse getUserById(String userId);
    List<UserResponse> getAllUsers();
    List<UserResponse> getUsersByCollegeId(UUID collegeId);
    boolean deleteUser(String userId);
    boolean activateUser(String userId);
    boolean deActivateUser(String userId);
    boolean deleteAllUserByCollegeId(UUID collegeId);
}
