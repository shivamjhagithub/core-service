package com.CoreService.CoreService.classroom.repository;

import com.CoreService.CoreService.classroom.entity.ClassroomPermission;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomPermissionRepository extends JpaRepository<ClassroomPermission, UUID> {

    List<ClassroomPermission> findAllByCollegeIdAndClassroom_Id(UUID collegeId, UUID classroomId);

    Optional<ClassroomPermission> findByCollegeIdAndClassroom_IdAndPermissionType(UUID collegeId,
                                                                                 UUID classroomId,
                                                                                 ClassroomPermissionType permissionType);

    @Modifying
    @Query("delete from ClassroomPermission p where p.collegeId = :collegeId and p.classroom.id = :classroomId")
    void deleteAllOfClassroom(@Param("collegeId") UUID collegeId, @Param("classroomId") UUID classroomId);
}
