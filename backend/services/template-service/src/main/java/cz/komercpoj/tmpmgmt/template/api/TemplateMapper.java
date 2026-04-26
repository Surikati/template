package cz.komercpoj.tmpmgmt.template.api;

import cz.komercpoj.tmpmgmt.template.api.dto.TemplateBundle;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateDraftResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse.DiffSummary;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionDiffResponse.VersionSnapshot;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionResponse;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.ImportBundle;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.ImportedVersion;
import cz.komercpoj.tmpmgmt.template.application.TemplateService.VersionDiff;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateDraftEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionEntity;
import java.time.Instant;
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

  default TemplateBundle toBundle(
      TemplateEntity template, TemplateDraftEntity draft, List<TemplateVersionEntity> versions) {
    var meta =
        new TemplateBundle.Metadata(
            template.getSlug(),
            template.getName(),
            template.getDescription(),
            template.getCategory(),
            arrayToList(template.getTags()));
    var draftDto = new TemplateBundle.Draft(draft.getContent(), draft.getVariablesSchema());
    // Versions ordered ascending so an importer replays them in the same chronological order
    // they were originally published in.
    var versionDtos =
        versions.stream()
            .sorted((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
            .map(
                v ->
                    new TemplateBundle.Version(
                        v.getVersionNumber(),
                        v.getContent(),
                        v.getVariablesSchema(),
                        v.getChangeNote(),
                        v.getPublishedAt()))
            .toList();
    return new TemplateBundle(
        TemplateBundle.CURRENT_SCHEMA_VERSION, Instant.now(), meta, draftDto, versionDtos);
  }

  default ImportBundle toImportCommand(TemplateBundle bundle, UUID importedBy) {
    var versions =
        bundle.versions().stream()
            .map(
                v ->
                    new ImportedVersion(
                        v.versionNumber(), v.content(), v.variablesSchema(), v.changeNote()))
            .toList();
    return new ImportBundle(
        bundle.template().slug(),
        bundle.template().name(),
        bundle.template().description(),
        bundle.template().category(),
        bundle.template().tags(),
        bundle.draft().content(),
        bundle.draft().variablesSchema(),
        versions,
        importedBy);
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
