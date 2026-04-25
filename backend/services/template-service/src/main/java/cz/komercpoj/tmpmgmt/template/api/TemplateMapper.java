package cz.komercpoj.tmpmgmt.template.api;

import cz.komercpoj.tmpmgmt.template.api.dto.TemplateDraftResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionResponse;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateDraftEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionEntity;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

  @Mapping(target = "tags", source = "tags", qualifiedByName = "arrayToList")
  TemplateResponse toResponse(TemplateEntity entity);

  TemplateDraftResponse toResponse(TemplateDraftEntity draft);

  TemplateVersionResponse toResponse(TemplateVersionEntity version);

  List<TemplateResponse> toTemplateResponses(List<TemplateEntity> entities);

  List<TemplateVersionResponse> toVersionResponses(List<TemplateVersionEntity> entities);

  @Named("arrayToList")
  default List<String> arrayToList(String[] array) {
    return array == null ? List.of() : Arrays.asList(array);
  }
}
