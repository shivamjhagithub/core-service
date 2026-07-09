package com.CoreService.CoreService.user.Contoller;

import com.CoreService.CoreService.common.Response.BasicResponse;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.user.Requests.UserRequsets;
import com.CoreService.CoreService.user.Responses.UserResponse;
import com.CoreService.CoreService.user.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserContext userContext;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequsets request) {

        UserResponse response = userService.createUser(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {

        String userId = userContext.getUserId();

        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable String userId) {

        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/college/{collegeId}")
    public ResponseEntity<List<UserResponse>> getUsersByCollegeId(
            @PathVariable UUID collegeId) {

        List<UserResponse> users = userService.getUsersByCollegeId(collegeId);

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<BasicResponse> deleteUser(
            @PathVariable String userId) {

        boolean ans =userService.deleteUser(userId);
        if(ans){
            return ResponseEntity.ok(BasicResponse.builder().success(true).message("User deleted successfully").build());
        }
        else{
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(BasicResponse.builder().success(false).message("User not found").build());

        }}

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<BasicResponse> activateUser(
            @PathVariable String userId) {

        boolean ans =userService.activateUser(userId);
        if(ans){
            return ResponseEntity.ok(BasicResponse.builder().success(true).message("User activated successfully").build());
        }
        else{
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(BasicResponse.builder().success(false).message("User activation unsuccessfull").build());

        }

    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<BasicResponse> deactivateUser(
            @PathVariable String userId) {

        userService.deActivateUser(userId);

        return ResponseEntity.ok(BasicResponse.builder().success(true).message("User is deactivated").build());
    }
}