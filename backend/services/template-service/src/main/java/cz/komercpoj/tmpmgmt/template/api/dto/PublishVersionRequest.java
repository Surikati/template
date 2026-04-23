package cz.komercpoj.tmpmgmt.template.api.dto;

import jakarta.validation.constraints.Size;

public record PublishVersionRequest(@Size(max = 5000) String changeNote) {}
