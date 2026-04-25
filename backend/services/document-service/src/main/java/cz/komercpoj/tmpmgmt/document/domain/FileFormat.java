package cz.komercpoj.tmpmgmt.document.domain;

public enum FileFormat {
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
  PDF("application/pdf", "pdf"),
  HTML("text/html", "html");

  private final String mimeType;
  private final String extension;

  FileFormat(String mimeType, String extension) {
    this.mimeType = mimeType;
    this.extension = extension;
  }

  public String mimeType() {
    return mimeType;
  }

  public String extension() {
    return extension;
  }
}
