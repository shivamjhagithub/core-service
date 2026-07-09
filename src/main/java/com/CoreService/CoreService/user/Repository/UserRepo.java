package com.CoreService.CoreService.user.Repository;

import com.CoreService.CoreService.user.Entities.UserEntity;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEntity,Integer> {
    UserEntity save(UserEntity userEntity);

    Optional<UserEntity> findById(String id);

    List<UserEntity> findAll();

    void deleteById(String id);

    List<UserEntity> findByCollegeId(UUID collegeId);

    boolean existsById(@NotBlank String userid);
}
