package com.CoreService.CoreService.user.Services;

import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import com.CoreService.CoreService.user.Requests.UserRequsets;
import com.CoreService.CoreService.user.Responses.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public  class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserResponse createUser(UserRequsets requsets) {
        if (userRepo.existsById(requsets.getUserid())) {
            throw new RuntimeException("User id already exists");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(requsets.getUserid());
        userEntity.setPassword(requsets.getPassword());
        userEntity.setEmail(requsets.getEmail());

        userRepo.save(userEntity);

        UserResponse response = new UserResponse();
        response.setUserId(userEntity.getUserId());
        response.setEmail(userEntity.getEmail());
        return response;
    }

    @Override
    public UserResponse getUserById(String userId) {

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse response = new UserResponse();

        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFatherName(user.getFatherName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAlternatePhoneNumber(user.getAlternatePhoneNumber());
        response.setBloodGroup(user.getBloodGroup());
        response.setCollegeId(user.getCollegeId());
        response.setImage(user.getImage());

        return response;    }

    @Override
    public List<UserResponse> getAllUsers() {

        List<UserEntity> users = userRepo.findAll();

        return users.stream().map(user -> {

            UserResponse response = new UserResponse();

            response.setUserId(user.getUserId());
            response.setEmail(user.getEmail());
            response.setFatherName(user.getFatherName());
            response.setPhoneNumber(user.getPhoneNumber());
            response.setAlternatePhoneNumber(user.getAlternatePhoneNumber());
            response.setBloodGroup(user.getBloodGroup());
            response.setCollegeId(user.getCollegeId());
            response.setImage(user.getImage());

            return response;

        }).toList();
    }

    @Override
    public List<UserResponse> getUsersByCollegeId(UUID collegeId) {

        List<UserEntity> users = userRepo.findByCollegeId(collegeId);

        return users.stream().map(user -> {

            UserResponse response = new UserResponse();

            response.setUserId(user.getUserId());
            response.setEmail(user.getEmail());
            response.setFatherName(user.getFatherName());
            response.setPhoneNumber(user.getPhoneNumber());
            response.setAlternatePhoneNumber(user.getAlternatePhoneNumber());
            response.setBloodGroup(user.getBloodGroup());
            response.setCollegeId(user.getCollegeId());
            response.setImage(user.getImage());

            return response;

        }).toList();
    }

    @Override
    public void deleteUser(String userId) {

        if (!userRepo.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        userRepo.deleteById(userId);
    }

    @Override
    public void activateUser(String userId) {

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActivate(true);

        userRepo.save(user);
    }

    @Override
    public void deActivateUser(String userId) {

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActivate(false);

        userRepo.save(user);
    }
}
