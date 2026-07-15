package com.CoreService.CoreService.College.Services;

import com.CoreService.CoreService.College.Dto.CollegeDto;
import com.CoreService.CoreService.College.Entities.CollegeEntity;
import com.CoreService.CoreService.College.Repository.CollegeRepository;
import com.CoreService.CoreService.College.Request.CollegeDataRequest;
import com.CoreService.CoreService.Permission.Services.RolePermissionService;
import com.CoreService.CoreService.module.Services.ModuleService;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Entities.UserRole;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import com.CoreService.CoreService.role.Repository.UserRoleRepository;
import com.CoreService.CoreService.role.Services.RoleService;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import com.CoreService.CoreService.user.Services.UserService;
import com.CoreService.CoreService.user.Services.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final UserServiceImpl userService;
    private final ModuleService moduleService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final UserRepo userRepo;
    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private PasswordEncoder passwordEncoder;
    public Boolean createCollege(CollegeDataRequest collegeDataRequest) {
        try{
            if(collegeDataRequest == null) {
                return false;
            }
            CollegeEntity collegeEntity=CollegeEntity
                    .builder()
                    .collegeAddress(collegeDataRequest.getCollegeAddress())
                    .collegeCity(collegeDataRequest.getCollegeCity())
                    .collegeCode(collegeDataRequest.getCollegeCode())
                    .collegeCountry(collegeDataRequest.getCollegeCountry())
                    .collegeEmail(collegeDataRequest.getCollegeEmail())
                    .collegeName(collegeDataRequest.getCollegeName())
                    .collegeDescription(collegeDataRequest.getCollegeDescription())
                    .collegePhone(collegeDataRequest.getCollegePhone())
                    .collegeState(collegeDataRequest.getCollegeState())
                    .collegeZip(collegeDataRequest.getCollegeZip())
                    .universityCode(collegeDataRequest.getUniversityCode())
                    .universityName(collegeDataRequest.getUniversityName())
                    .build();
            CollegeEntity collegeEntity1= collegeRepository.save(collegeEntity);
            UserEntity collegeAdmin= UserEntity.builder().userName(collegeEntity1.getCollegeName()+"ADMIN").userId(collegeEntity1.getCollegeName()+"00001").email(collegeDataRequest.getAdminEmail()).password(passwordEncoder.encode("CollegeAdminPassword"+collegeEntity1.getCollegeName())).build();
            Role role = Role.builder().college(collegeEntity1).roleName("COLLEGE_ADMIN").roleDescription("this is a college admin for college"+collegeEntity1.getCollegeName()).build();
            UserRole userRole=UserRole.builder().user(collegeAdmin).role(role).build();
            userRepo.save(collegeAdmin);
            roleRepo.save(role);
            userRoleRepo.save(userRole);

            //send mail to admin

            if(collegeEntity1 != null) {
                return true;
            }
            return false;

        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public Page<CollegeDto> getAllCollege( int pageNo, int pageSize ) {
        Pageable pageable = PageRequest.of( pageNo, pageSize );
        return collegeRepository .findAll(pageable) .map(this::mapToDto);
    }
    public CollegeDto getCollegeByCollegeId(UUID collegeId){
        try{
            Optional<CollegeEntity> collegeOptional = collegeRepository.findByCollegeId(collegeId);
            if(collegeOptional.isPresent()){
                CollegeEntity collegeEntity=collegeOptional.get();
                return CollegeDto.builder()
                        .collegeDescription(collegeEntity.getCollegeDescription())
                        .collegeName(collegeEntity.getCollegeName())
                        .collegeCode(collegeEntity.getCollegeCode())
                        .universityName(collegeEntity.getUniversityName())
                        .collegeId(collegeEntity.getCollegeId())
                        .collegePhone(collegeEntity.getCollegePhone())
                        .collegeEmail(collegeEntity.getCollegeEmail())
                        .collegeState(collegeEntity.getCollegeState())
                        .collegeZip(collegeEntity.getCollegeZip())
                        .collegeAddress(collegeEntity.getCollegeAddress())
                        .collegeCode(collegeEntity.getCollegeCode())
                        .build();
            }
            else{
                return null;
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public CollegeDto updateCollegeData(UUID collegeId, CollegeDataRequest collegeDataRequest){
        try{
            Optional<CollegeEntity> collegeOptional = collegeRepository.findByCollegeId(collegeId);
            if(collegeOptional.isPresent()){
                CollegeEntity collegeEntity=collegeOptional.get();
                if(collegeDataRequest.getCollegeAddress()!=null){
                    collegeEntity.setCollegeAddress(collegeDataRequest.getCollegeAddress());
                }
                if(collegeDataRequest.getCollegeCity()!=null){
                    collegeEntity.setCollegeCity(collegeDataRequest.getCollegeCity());
                }
                if(collegeDataRequest.getCollegePhone()!=null){
                    collegeEntity.setCollegePhone(collegeDataRequest.getCollegePhone());
                }
                if(collegeDataRequest.getCollegeState()!=null){
                    collegeEntity.setCollegeState(collegeDataRequest.getCollegeState());
                }
                if(collegeDataRequest.getCollegeZip()!=null){
                    collegeEntity.setCollegeZip(collegeDataRequest.getCollegeZip());
                }
                if(collegeDataRequest.getCollegeCountry()!=null){
                    collegeEntity.setCollegeCountry(collegeDataRequest.getCollegeCountry());
                }
                if(collegeDataRequest.getCollegeEmail()!=null){
                    collegeEntity.setCollegeEmail(collegeDataRequest.getCollegeEmail());
                }
                if(collegeDataRequest.getCollegeName()!=null){
                    collegeEntity.setCollegeName(collegeDataRequest.getCollegeName());
                }
                collegeRepository.save(collegeEntity);
                return CollegeDto.builder()
                        .collegeDescription(collegeEntity.getCollegeDescription())
                        .collegeName(collegeEntity.getCollegeName())
                        .collegeCode(collegeEntity.getCollegeCode())
                        .universityName(collegeEntity.getUniversityName())
                        .collegeId(collegeEntity.getCollegeId())
                        .collegePhone(collegeEntity.getCollegePhone())
                        .collegeEmail(collegeEntity.getCollegeEmail())
                        .collegeState(collegeEntity.getCollegeState())
                        .collegeZip(collegeEntity.getCollegeZip())
                        .collegeAddress(collegeEntity.getCollegeAddress())
                        .collegeCode(collegeEntity.getCollegeCode())
                        .build();
            }
            else{
                return null;
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public boolean deleteCollegeData(UUID collegeId){
        try{
            if(collegeId==null){
                return false;
            }
            collegeRepository.deleteById(collegeId);
            // delete all users of college
            userService.deleteAllUsersOfCollegeByCollegeId(collegeId);
            //delete all roles of college
            roleService.deleteAllRolesOfCollege(collegeId);
            // delete all permissions of college
            rolePermissionService.deleteAllRolesPermissionOfCollege(collegeId);
            //delete all modules of college
            moduleService.deleteCollegeModule(collegeId);
            return true;
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public CollegeDto mapToDto( CollegeEntity collegeEntity ) {
        return CollegeDto
                .builder()
                .collegeId( collegeEntity.getCollegeId() )
                .collegeName( collegeEntity.getCollegeName() )
                .collegeCode( collegeEntity.getCollegeCode() )
                .collegeDescription( collegeEntity.getCollegeDescription() )
                .collegeEmail( collegeEntity.getCollegeEmail() )
                .collegePhone( collegeEntity.getCollegePhone() )
                .collegeAddress( collegeEntity.getCollegeAddress() )
                .collegeState( collegeEntity.getCollegeState() )
                .collegeZip( collegeEntity.getCollegeZip() )
                .universityName( collegeEntity.getUniversityName() )
                .build();
    }
    public Page<CollegeDto> searchColleges(String collegeName, String universityName, String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (collegeName != null && universityName != null) {

            return collegeRepository
                    .findByCollegeNameContainingIgnoreCaseAndUniversityNameContainingIgnoreCase(
                            collegeName, universityName, pageable).map(this::mapToDto);
        }
        if (collegeName != null) {
            return collegeRepository.findByCollegeNameContainingIgnoreCase(
                            collegeName, pageable).map(this::mapToDto);
        }
        if (universityName != null) {
            return collegeRepository.findByUniversityNameContainingIgnoreCase(universityName, pageable).map(this::mapToDto);
        }
        if (city != null) {
            return collegeRepository
                    .findByCollegeCityContainingIgnoreCase(city, pageable)
                    .map(this::mapToDto);
        }

        return collegeRepository
                .findAll(pageable)
                .map(this::mapToDto);
    }
}
