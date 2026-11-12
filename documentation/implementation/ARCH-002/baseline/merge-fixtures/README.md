# Merge Fixture Notes

The three inputs share accession `P12345`. `detailed.dat` uses UniProt `entry_version` 3; `stale-lower-entry-version.dat`
uses version 2 and should be counted as stale after version 3 has been imported. The light TSV row intentionally omits
detail fields and source version; on a supported import path those absent values must not erase the detailed record.

**Current limitation:** the checked-in TSV reader skips the header but passes each following row to
`ProteinEntryItemProcessor`, which expects a complete tagged UniProt DAT record and creates a fresh parse context per
row. The TSV row therefore cannot currently produce a valid `ProteinEntry`. This fixture records the intended light
input, not a passing merge test. Keep P0 task 0.9 open until TSV parsing is corrected or the approved light-file
contract changes; do not report merge counts from this fixture yet.

The DAT records are small deterministic probes, not a substitute for the PERF-001 throughput fixture. A merge run
must verify preserved fields/children, `updated` and `stale` counters, technical `revision`, and event/outbox behavior
against the P4 implementation.