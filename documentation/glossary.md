# Glossary

Authoritative definitions for all domain and technical terms used in this project's documentation. Any team member who encounters an unfamiliar term should resolve it here before raising a question.

---

| Term | Definition |
|---|---|
| **Accession** | A stable, unique identifier assigned by UniProtKB to each protein entry. Format: `[OPQ][0-9][A-Z0-9]{3}[0-9]` (6 chars) or `[A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9]` (6 chars) or a 10-character secondary form. Example: `Q6GZX4`. |
| **Entry** | One protein record in UniProtKB, delimited by `//` in a `.dat` flat file. Corresponds to one row in `protein_entry`. |
| **Entry Name** | A human-readable mnemonic identifier of the form `GENENAME_ORGANISM` (e.g. `001R_FRG3G`). Stable but not guaranteed unique across releases. |
| **Reviewed** | A protein entry that has been manually curated by a UniProt scientist and included in Swiss-Prot. `reviewed = TRUE` in the database. Contrast with Unreviewed. |
| **Unreviewed** | A protein entry that was automatically imported into TrEMBL without manual curation. `reviewed = FALSE`. |
| **Evidence Level** | A score (1–5) indicating the strength of experimental evidence for a protein's existence. 1 = Protein level (strongest); 5 = Uncertain (weakest). Stored in `evidence_level`. |
| **Swiss-Prot** | The manually reviewed, high-quality section of UniProtKB. All entries from Swiss-Prot have `reviewed = TRUE`. |
| **TrEMBL** | The automatically annotated section of UniProtKB (`reviewed = FALSE`). Much larger than Swiss-Prot. |
| **UniProtKB** | UniProt Knowledgebase — the primary source database for protein sequence and functional information maintained by the UniProt Consortium. |
| **`.dat` file** | The UniProt flat-file format. One file contains all entries, each ending with `//`. The full Swiss-Prot file is named `uniprot_sprot.dat`. |
| **`.tsv` file** | A tab-separated-values export from UniProt. Simpler to parse than `.dat` but contains fewer fields. |
| **GO Term** | A Gene Ontology term — a controlled vocabulary entry classifying a protein's biological process (P), molecular function (F), or cellular component (C). Identified by a `GO:` prefixed ID (e.g. `GO:0046782`). |
| **GO Aspect** | The branch of Gene Ontology a term belongs to: `P` (Biological Process), `F` (Molecular Function), `C` (Cellular Component). |
| **Feature** | An annotated region of a protein sequence (e.g. CHAIN, DOMAIN, SIGNAL, BINDING). Stored in `protein_feature`, mapped from `FT` lines in `.dat`. |
| **Cross Reference** | A link from a protein entry to an external database record (e.g. EMBL, RefSeq, KEGG, Pfam, InterPro). Stored in `cross_reference`, mapped from `DR` lines. |
| **Taxon / TaxID** | NCBI Taxonomy identifier — a positive integer uniquely identifying a biological taxon. Example: `9606` = Homo sapiens. Stored in `taxid`. |
| **Lineage** | The taxonomic hierarchy above a protein's organism (e.g. `["Viruses", "Varidnaviria", ...]`). Stored as a `TEXT[]` array in `lineage`. |
| **Materialized View** | A PostgreSQL object that stores the result of a query physically on disk. Used to pre-aggregate dashboard KPIs and chart data. Must be refreshed (`REFRESH MATERIALIZED VIEW CONCURRENTLY`) after each import. |
| **JPA Specification** | A Spring Data JPA interface (`Specification<T>`) that encapsulates a single predicate. Multiple specifications are combined with `.and()` / `.or()` to build dynamic multi-filter queries without raw SQL. |
| **Spring Batch** | The Spring framework module used to implement the import pipeline. Provides chunk-oriented processing, retry, skip, and job restart capabilities. |
| **Chunk** | A unit of work in Spring Batch. The import job processes records in chunks of 500. Each chunk is committed atomically; a failed chunk rolls back only itself. |
| **Import Job** | A Spring Batch `JobExecution` record representing one triggered import. Has a status: `RUNNING`, `COMPLETED`, or `FAILED`. |
| **DTO** | Data Transfer Object — a plain Java class used to transfer data between the controller and service layers, or between the backend and frontend. Never exposes JPA entities directly. |
| **tsvector** | A PostgreSQL data type that stores a pre-processed, lexeme-normalized representation of text for use with full-text search (`@@` operator). Maintained by a trigger on `protein_entry`. |
| **GIN Index** | Generalized Inverted Index — a PostgreSQL index type suited for array columns and `tsvector`. Used on `gene_name_synonyms`, `lineage`, and `search_vector`. |
| **Overwrite Strategy** | The import duplicate-handling policy: an existing row with a matching `accession` is updated with all fields from the new file. `created_at` is preserved; `updated_at` is refreshed. |
| **Stale Entry** | A `protein_entry` row present in the database but absent from the most recently imported file. Marked with `stale = TRUE` (not deleted) to preserve data lineage. |
| **JWT** | JSON Web Token — a signed, self-contained token used for authentication. The backend issues access tokens (1-hour expiry) and refresh tokens (24-hour expiry). |
| **ROLE_ADMIN** | Spring Security authority granted to administrative users. Required for all `/api/admin/**` endpoints. |
| **ROLE_USER** | Spring Security authority granted to standard authenticated users. Required for all non-admin protected endpoints. |
| **MVP** | Minimum Viable Product — the smallest set of features that delivers core value. Defined in Overview.md §17. |
| **NFR** | Non-Functional Requirement — a constraint on system quality attributes (performance, scalability, security) rather than on behaviour. Defined in Overview.md §12. |
| **SDD** | Spec-Driven Development — a development approach in which all functional and non-functional requirements are fully specified before implementation begins, enabling parallel frontend/backend development and objective verification. |
| **Definition of Done** | A checklist that must be satisfied before a user story or endpoint is considered complete. Defined per story in Overview.md §11 and per endpoint in api-contract.md §6. |
