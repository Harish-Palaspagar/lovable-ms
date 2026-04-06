package com.harish.mapper;

import com.harish.dto.UserDto;
import com.harish.dto.auth.SignupRequest;
import com.harish.dto.auth.UserProfileResponse;
import com.harish.entity.User;
import com.harish.security.JwtUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(SignupRequest signupRequest);

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);

    UserDto toUserDto(User user);

}
