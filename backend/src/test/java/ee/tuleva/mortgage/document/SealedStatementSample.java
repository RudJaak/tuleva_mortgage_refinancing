package ee.tuleva.mortgage.document;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A real bank-sealed statement holds personal data, so it is never committed. Tests that need one
 * read it from {@code SEALED_STATEMENT_SAMPLE} (default: the gitignored {@code konto_vv.asice} in
 * the repo root) and are skipped when it is missing.
 */
final class SealedStatementSample {

  private SealedStatementSample() {}

  static byte[] bytes() throws IOException {
    Path path =
        Path.of(System.getenv().getOrDefault("SEALED_STATEMENT_SAMPLE", "../konto_vv.asice"));
    assumeTrue(Files.isReadable(path), "No sealed statement sample at " + path.toAbsolutePath());
    return Files.readAllBytes(path);
  }

  /** Rewrites the container, letting {@code dataFile} change the first non-META-INF entry. */
  static byte[] withDataFile(byte[] container, UnaryOperator<byte[]> dataFile) throws IOException {
    return rewrite(container, dataFile, null, null);
  }

  static byte[] withExtraFile(byte[] container, String name, byte[] content) throws IOException {
    return rewrite(container, UnaryOperator.identity(), name, content);
  }

  private static byte[] rewrite(
      byte[] container, UnaryOperator<byte[]> dataFile, String extraName, byte[] extraContent)
      throws IOException {
    var out = new ByteArrayOutputStream();
    try (var zipIn = new ZipInputStream(new ByteArrayInputStream(container));
        var zipOut = new ZipOutputStream(out)) {
      boolean changed = false;
      for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
        byte[] content = zipIn.readAllBytes();
        if (!changed
            && !entry.getName().equals("mimetype")
            && !entry.getName().startsWith("META-INF/")) {
          content = dataFile.apply(content);
          changed = true;
        }
        write(zipOut, entry.getName(), content, entry.getName().equals("mimetype"));
      }
      if (extraName != null) {
        write(zipOut, extraName, extraContent, false);
      }
    }
    return out.toByteArray();
  }

  private static void write(ZipOutputStream zip, String name, byte[] content, boolean stored)
      throws IOException {
    var entry = new ZipEntry(name);
    if (stored) {
      var crc = new CRC32();
      crc.update(content);
      entry.setMethod(ZipEntry.STORED);
      entry.setSize(content.length);
      entry.setCrc(crc.getValue());
    }
    zip.putNextEntry(entry);
    zip.write(content);
    zip.closeEntry();
  }
}
