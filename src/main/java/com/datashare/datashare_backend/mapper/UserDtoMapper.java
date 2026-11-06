package com.datashare.datashare_backend.mapper;

import com.datashare.datashare_backend.dto.RegisterDTO;
import com.datashare.datashare_backend.entities.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "files", ignore = true)
    User toEntity(RegisterDTO registerDTO);
}