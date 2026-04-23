package cz.komercpoj.tmpmgmt.rendering.api.dto;

public record RenderResponse(RenderFormat format, String filename, byte[] content) {}
