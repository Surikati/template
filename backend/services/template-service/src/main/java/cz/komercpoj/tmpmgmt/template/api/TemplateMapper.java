package cz.komercpoj.tmpmgmt.template.api;

import cz.komercpoj.tmpmgmt.template.api.dto.TemplateDraftResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse.DiffSummary;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse.VersionSnapshot;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionResponse;
import cz.komercpoj.tmpmgmt.template.application.TemplateService.VersionDiff;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateDraftEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

  default TemplateVersionDiffResponse toDiffResponse(UUID templateId, VersionDiff diff) {
    var from = diff.from();
    var to = diff.to();
    return new TemplateVersionDiffResponse(
        templateId,
        new VersionSnapshot(
            from.getVersionNumber(),
            from.getContent(),
            from.getVariablesSchema(),
            from.getChangeNote(),
            from.getPublishedAt(),
            from.getPublishedBy()),
        new VersionSnapshot(
            to.getVersionNumber(),
            to.getContent(),
            to.getVariablesSchema(),
            to.getChangeNote(),
            to.getPublishedAt(),
            to.getPublishedBy()),
        new DiffSummary(
            !Objects.equals(from.getContent(), to.getContent()),
            !Objects.equals(from.getVariablesSchema(), to.getVariablesSchema())));
  }
}
