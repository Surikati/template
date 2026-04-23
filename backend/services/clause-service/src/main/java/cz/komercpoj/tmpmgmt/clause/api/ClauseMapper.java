package cz.komercpoj.tmpmgmt.clause.api;

import cz.komercpoj.tmpmgmt.clause.api.dto.ClauseResponse;
import cz.komercpoj.tmpmgmt.clause.api.dto.ClauseVersionResponse;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseEntity;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseVersionEntity;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ClauseMapper {

    @Mapping(target = "tags", source = "tags", qualifiedByName = "arrayToList")
    ClauseResponse toResponse(ClauseEntity entity);

    ClauseVersionResponse toResponse(ClauseVersionEntity version);

    List<ClauseResponse> toClauseResponses(List<ClauseEntity> entities);

    List<ClauseVersionResponse> toVersionResponses(List<ClauseVersionEntity> entities);

    @Named("arrayToList")
    default List<String> arrayToList(String[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }
}
