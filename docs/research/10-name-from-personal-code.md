# 10 · Looking up a person's name from their personal code

**Question:** can the demo show a person's real name from the personal code they type, like DigiDoc4 does when encrypting for someone?

**Answer:** yes, through the public certificate directories (LDAP) of the two Estonian trust service providers. Checked live on 2026-10-10 (confidence: high for availability and query shape; the coverage notes below come from the providers' documentation).

## The directories

| Provider | Covers | Host | Base DN |
|---|---|---|---|
| SK ID Solutions | ID cards, Digi-ID, residence cards, e-resident Digi-ID and Mobile-ID certificates issued by SK (cards made before November 2025) | `ldaps://esteid.ldap.sk.ee` | `c=EE` |
| Zetes (Thales cards) | ID cards made by Thales since November 2025 | `ldaps://ldap.eidpki.ee` | `dc=ldap,dc=eidpki,dc=ee` |

- Both directories allow anonymous binds over LDAPS only.
- **Search filter:** `(serialNumber=PNOEE-<personal code>)`, or the exact CN.
- **Name format:** the entry's `cn` is `SURNAME,GIVENNAME,PERSONALCODE`.
- **Wildcard filters are refused** (`Protocol error`). You need the exact code, which prevents fishing for names.
- **Result cap:** at most 50 entries per answer.
- **Removed certificates:** suspended, revoked and expired certificates are removed, so only people with a currently valid certificate are found.
- **Not covered: Smart-ID.** Its certificates are not in either directory. Someone who only uses Smart-ID, or whose ID card has expired, will not be found.
- **Search both directories.** Since November 2025 a person may appear only in the Zetes one. A test lookup found nothing in SK's directory and the person in Zetes'.

```
ldapsearch -H ldaps://esteid.ldap.sk.ee/ -x -b "c=EE" "(serialNumber=PNOEE-<code>)" cn
ldapsearch -H ldaps://ldap.eidpki.ee/ -x -b "dc=ldap,dc=eidpki,dc=ee" "(serialNumber=PNOEE-<code>)" cn
```

## Connecting from Java

SK's directory only negotiates TLS 1.2 with RSA key exchange (`AES256-GCM-SHA384`). Java 24 and later disable that family by default (`TLS_RSA_*` in `jdk.tls.disabledAlgorithms`), and the list cannot be relaxed per connection, so the handshake fails. The backend removes only that entry at startup (`EidDirectoryTls`). Zetes' directory works with the defaults.

## Considerations

- **Purpose.** The directories exist so that anyone can encrypt a document for a person. Using them to greet a demo user by name is a different purpose. The data is public, but this is still processing of personal data.
- **Abuse.** A page that turns any typed personal code into a name is a lookup oracle. Personal codes are guessable: birth date plus a check digit. Mitigations:
  - look the name up only during a login the person starts with their own code and phone number;
  - never store or log the result beyond the session name;
  - rate-limit lookups.
- **Not authentication.** The demo login is a stub. Anyone can type someone else's code and see that person's name. Acceptable for an internal demo, not for a public one.
- **Alternatives:**
  - The population register (Rahvastikuregister, over X-tee) needs X-tee membership and a legal basis, so it is not feasible here.
  - Real Mobile-ID or Smart-ID returns the verified name, but needs a relying-party contract with SK.

Sources: [SK LDAP](https://www.skidsolutions.eu/resources/ldap/), [SK LDAP knowledge base](https://github.com/SK-EID/LDAP/wiki/Knowledge-Base), [ID.ee: Thales ID card](https://www.id.ee/en/article/thales-id-card/).
