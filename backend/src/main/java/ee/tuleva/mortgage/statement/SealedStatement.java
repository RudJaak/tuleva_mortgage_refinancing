package ee.tuleva.mortgage.statement;

import java.time.Instant;
import java.util.List;

/** A statement container whose seal was verified as a known Estonian bank's. */
record SealedStatement(
    String bankName,
    String bankRegistryCode,
    String sealCommonName,
    Instant sealedAt,
    List<File> files) {

  record File(String name, String mediaType, byte[] content) {}
}
