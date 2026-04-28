# Uniprot Swiss-Prot
### Source : https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz
## Example :
```dat

ID   001R_FRG3G              Reviewed;         256 AA.
AC   Q6GZX4;
DT   28-JUN-2011, integrated into UniProtKB/Swiss-Prot.
DT   19-JUL-2004, sequence version 1.
DT   28-JAN-2026, entry version 46.
DE   RecName: Full=Putative transcription factor 001R;
GN   ORFNames=FV3-001R;
OS   Frog virus 3 (isolate Goorha) (FV-3).
OC   Viruses; Varidnaviria; Bamfordvirae; Nucleocytoviricota; Megaviricetes;
OC   Pimascovirales; Pimascovirales incertae sedis; Iridoviridae;
OC   Alphairidovirinae; Ranavirus; Ranavirus rana1; Frog virus 3.
OX   NCBI_TaxID=654924;
OH   NCBI_TaxID=30343; Dryophytes versicolor (chameleon treefrog).
OH   NCBI_TaxID=8404; Lithobates pipiens (Northern leopard frog) (Rana pipiens).
OH   NCBI_TaxID=45438; Lithobates sylvaticus (Wood frog) (Rana sylvatica).
OH   NCBI_TaxID=8316; Notophthalmus viridescens (Eastern newt) (Triturus viridescens).
RN   [1]
RP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].
RX   PubMed=15165820; DOI=10.1016/j.virol.2004.02.019;
RA   Tan W.G., Barkman T.J., Gregory Chinchar V., Essani K.;
RT   "Comparative genomic analyses of frog virus 3, type species of the genus
RT   Ranavirus (family Iridoviridae).";
RL   Virology 323:70-84(2004).
CC   -!- FUNCTION: Transcription activation. {ECO:0000305}.
CC   ---------------------------------------------------------------------------
CC   Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms
CC   Distributed under the Creative Commons Attribution (CC BY 4.0) License
CC   ---------------------------------------------------------------------------
DR   EMBL; AY548484; AAT09660.1; -; Genomic_DNA.
DR   RefSeq; YP_031579.1; NC_005946.1.
DR   SwissPalm; Q6GZX4; -.
DR   KEGG; vg:2947773; -.
DR   Proteomes; UP000008770; Segment.
DR   GO; GO:0046782; P:regulation of viral transcription; IEA:InterPro.
DR   InterPro; IPR007031; Poxvirus_VLTF3.
DR   Pfam; PF04947; Pox_VLTF3; 1.
PE   4: Predicted;
KW   Activator; Reference proteome; Transcription; Transcription regulation.
FT   CHAIN           1..256
FT                   /note="Putative transcription factor 001R"
FT                   /id="PRO_0000410512"
SQ   SEQUENCE   256 AA;  29735 MW;  B4840739BF7D4121 CRC64;
MAFSAEDVLK EYDRRRRMEA LLLSLYYPND RKLLDYKEWS PPRVQVECPK APVEWNNPPS
EKGLIVGHFS GIKYKGEKAQ ASEVDVNKMC CWVSKFKDAM RRYQGIQTCK IPGKVLSDLD
AKIKAYNLTV EGVEGFVRYS RVTKQHVAAF LKELRHSKQY ENVNLIHYIL TDKRVDIQHL
EKDLVKDFKA LVESAHRMRQ GHMINVKYIL YQLLKKHGHG PDGPDILTVK TGSKGVLYDD
SFRKIYTDLG WKFTPL
//

```
## UniProt flat file format

### Big Picture

The snippet above is **one protein entry**, ending with:

```text
//
```

That means:

* Everything before `//` = one protein record
* Next `ID ...` starts another protein entry

So this file is basically:

```text
Entry 1
//
Entry 2
//
Entry 3
//
...
```

---

# Structure of One Entry

Here is how to interpret the fields from your example.

---

## 1. Identification

```text
ID   001R_FRG3G              Reviewed;         256 AA.
```

### Meaning:

* **001R_FRG3G** = Entry name
* **Reviewed** = manually curated Swiss-Prot record
* **256 AA** = protein length = 256 amino acids

---

## 2. Accession Number

```text
AC   Q6GZX4;
```

### Meaning:

Stable unique identifier for this protein.

This acts as the primary key for the entry.

---

## 3. Dates

```text
DT   28-JUN-2011, integrated into UniProtKB/Swiss-Prot.
DT   19-JUL-2004, sequence version 1.
DT   28-JAN-2026, entry version 46.
```

### Meaning:

* added to Swiss-Prot in 2011
* sequence first version in 2004
* metadata updated 46 times

---

## 4. Description / Protein Name

```text
DE   RecName: Full=Putative transcription factor 001R;
```

### Meaning:

Recommended protein name.

---

## 5. Gene Name

```text
GN   ORFNames=FV3-001R;
```

### Meaning:

Gene / ORF that encodes this protein.

---

## 6. Organism Source

```text
OS   Frog virus 3 (isolate Goorha) (FV-3).
```

### Meaning:

Protein comes from this organism.

---

## 7. Taxonomy Classification

```text
OC   Viruses; Varidnaviria; ...
```

### Meaning:

Taxonomic lineage.

Like:

Kingdom → phylum → class → family → genus → species

---

## 8. Taxonomy ID

```text
OX   NCBI_TaxID=654924;
```

NCBI taxonomy identifier for organism.

---

## 9. Host Organisms

```text
OH   NCBI_TaxID=30343; Dryophytes versicolor ...
OH   ...
```

### Meaning:

This virus infects these hosts.

---

## 10. References / Publications

```text
RN   [1]
RP   NUCLEOTIDE SEQUENCE ...
RX   PubMed=15165820; DOI=...
RA   Tan W.G....
RT   "Comparative genomic analyses..."
RL   Virology 323:70-84(2004).
```

### Meaning:

Scientific article supporting annotation.

* RN = reference number
* RP = what was studied
* RX = PubMed / DOI
* RA = authors
* RT = title
* RL = journal citation

---

## 11. Comments

```text
CC   -!- FUNCTION: Transcription activation.
```

### Meaning:

Functional annotation.

This protein likely activates transcription.

---

## 12. Cross References

```text
DR   EMBL; AY548484; ...
DR   RefSeq; YP_031579.1; ...
DR   KEGG; ...
DR   GO; GO:0046782; ...
DR   Pfam; PF04947; ...
```

### Meaning:

Links to other databases.

Examples:

* EMBL = nucleotide sequence db
* RefSeq = NCBI protein/genome
* GO = Gene Ontology
* Pfam = protein family/domain
* InterPro = domain classification

---

## 13. Protein Evidence

```text
PE   4: Predicted;
```

Evidence levels:

1. Protein level evidence
2. Transcript evidence
3. Homology
4. Predicted
5. Uncertain

So here: computationally predicted.

---

## 14. Keywords

```text
KW   Activator; Reference proteome; Transcription;
```

Useful tags/categories.

---

## 15. Feature Table

```text
FT   CHAIN           1..256
FT                   /note="Putative transcription factor 001R"
FT                   /id="PRO_0000410512"
```

### Meaning:

Annotated sequence features.

Here:

* Mature chain spans aa 1 to 256

Other entries may include:

* DOMAIN
* REGION
* BINDING
* SIGNAL
* MUTAGENESIS
* VARIANT

---

## 16. Sequence Header

```text
SQ   SEQUENCE   256 AA;  29735 MW;  ... CRC64;
```

### Meaning:

* 256 amino acids
* molecular weight = 29,735 Da
* checksum

---

## 17. Actual Amino Acid Sequence

```text
MAFSAEDVLK EYDRRRRMEA ...
```

Protein sequence.

Spaces are only formatting.

Actual sequence:

```text
MAFSAEDVLKEYDRRRRMEA...
```

---

# How to Parse Programmatically

Each record separated by `//`

Each line starts with tag:

```python
ID
AC
DE
GN
OS
OC
OX
OH
RN
RP
RX
RA
RT
RL
CC
DR
PE
KW
FT
SQ
```

Then content after column 5.

---

# Practical Use Cases

Common use cases for this file:

### Build protein databases

Extract:

* accession
* organism
* sequence
* function

### Convert to FASTA

Take `AC` + `SQ`

### Bioinformatics pipelines

* protein family analysis
* GO annotation
* taxonomy mapping

