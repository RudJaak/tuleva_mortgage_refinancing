package ee.tuleva.mortgage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EidDirectoryTlsTest {

  @Test
  void removesOnlyRsaKeyExchangeFromTheDisabledList() {
    String disabled = "SSLv3, TLSv1, RC4, ECDH, TLS_RSA_*, rsa_pkcs1_sha1 usage HandshakeSignature";

    assertThat(EidDirectoryTls.without(disabled, EidDirectoryTls.RSA_KEY_EXCHANGE))
        .isEqualTo("SSLv3, TLSv1, RC4, ECDH, rsa_pkcs1_sha1 usage HandshakeSignature");
  }
}
