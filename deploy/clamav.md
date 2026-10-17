# ClamAV Malware Scanning Specification

## 1. Role & Operational Model

- **Role:** Streams uploaded media through an isolated anti-malware engine before making assets available in the public catalog or workspace.
- **Image:** `clamav/clamav:1.4.1` (Pinned version).
- **Protocol:** TCP streaming protocol (`INSTREAM` / `PING`) on port `3310`.
- **Network Topology:** Private container network only. Never exposed to public internet.

---

## 2. Fail-Closed Security Policy

In production environments, `ProductionConfigurationValidator` enforces strict upload protection:
- `REQUIRE_CLEAN_SCAN=true`: Any media upload whose scan returns non-clean (infected, timed out, or scanner unreachable) is rejected fail-closed and quarantined.
- `MALWARE_SCANNER_PROVIDER=fake`: Strictly forbidden on production profile; application aborts startup if detected.

---

## 3. Resource Requirements

- **Memory:** Minimum 1.5 GB RAM (ClamAV virus database definition footprint in memory is ~1.2 GB). Recommended: 2.0 GB RAM.
- **CPU:** 1.0 vCPU recommended for streaming scans.
- **Healthcheck:**
```bash
CMD-SHELL echo PING | nc -w 2 127.0.0.1 3310 | grep -q PONG || exit 1
```
