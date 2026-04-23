package cz.komercpoj.tmpmgmt.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Transport-level pagination request, decoupled from Spring Data types. */
public record PageRequest(@Min(0) int page, @Min(1) @Max(200) int size, String sort) {

    public PageRequest {
        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null);
    }
}
