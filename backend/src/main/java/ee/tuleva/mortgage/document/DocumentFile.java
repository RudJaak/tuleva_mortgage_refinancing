package ee.tuleva.mortgage.document;

/** One file from an upload: the upload itself, or a data file inside a signed container. */
public record DocumentFile(String name, String mediaType, byte[] content) {

  public static final String PDF = "application/pdf";

  public boolean isPdf() {
    return PDF.equalsIgnoreCase(mediaType) || name.toLowerCase().endsWith(".pdf");
  }
}
