### endpoint A : https://rest.uniprot.org/uniprotkb/search?facets=reviewed%2Cmodel_organism%2Cproteins_with%2Cexistence%2Cannotation_score%2Clength&query=*&size=0

return

```json 

{
  "facets": [
    {
      "label": "Status",
      "name": "reviewed",
      "allowMultipleSelection": false,
      "values": [
        {
          "label": "Reviewed (Swiss-Prot)",
          "value": "true",
          "count": 575503
        },
        {
          "label": "Unreviewed (TrEMBL)",
          "value": "false",
          "count": 149234636
        }
      ]
    },
    {
      "label": "Popular organisms",
      "name": "model_organism",
      "allowMultipleSelection": true,
      "values": [
        {
          "label": "Human",
          "value": "9606",
          "count": 210709
        },
        {
          "label": "Mouse",
          "value": "10090",
          "count": 87782
        },
        {
          "label": "Zebrafish",
          "value": "7955",
          "count": 78191
        },
        {
          "label": "Rat",
          "value": "10116",
          "count": 61540
        },
        {
          "label": "Bovine",
          "value": "9913",
          "count": 61360
        }
      ]
    },
    {
      "label": "Proteins with",
      "name": "proteins_with",
      "allowMultipleSelection": true,
      "values": [
        {
          "label": "3D structure",
          "value": "1",
          "count": 74161
        },
        {
          "label": "Active site",
          "value": "2",
          "count": 6822715
        },
        {
          "label": "Activity regulation",
          "value": "3",
          "count": 257479
        },
        {
          "label": "Allergen",
          "value": "4",
          "count": 974
        },
        {
          "label": "Alternative products (isoforms)",
          "value": "5",
          "count": 26028
        },
        {
          "label": "Alternative splicing",
          "value": "6",
          "count": 22777
        },
        {
          "label": "Beta strand",
          "value": "7",
          "count": 32736
        },
        {
          "label": "Binary interaction",
          "value": "8",
          "count": 29167
        },
        {
          "label": "Binding site",
          "value": "9",
          "count": 11345996
        },
        {
          "label": "Biophysicochemical properties",
          "value": "10",
          "count": 12503
        },
        {
          "label": "Biotechnological use",
          "value": "11",
          "count": 2355
        },
        {
          "label": "Catalytic activity",
          "value": "13",
          "count": 20450316
        },
        {
          "label": "Chain",
          "value": "14",
          "count": 12685703
        },
        {
          "label": "Cofactors",
          "value": "15",
          "count": 12510627
        },
        {
          "label": "Coiled-coil",
          "value": "16",
          "count": 6194216
        },
        {
          "label": "Compositional bias",
          "value": "17",
          "count": 34511290
        },
        {
          "label": "Cross-link",
          "value": "18",
          "count": 70886
        },
        {
          "label": "Developmental stage",
          "value": "19",
          "count": 14840
        },
        {
          "label": "Disease",
          "value": "20",
          "count": 5870
        },
        {
          "label": "Disruption phenotype",
          "value": "21",
          "count": 23993
        },
        {
          "label": "Disulfide bond",
          "value": "22",
          "count": 1257743
        },
        {
          "label": "DNA binding",
          "value": "23",
          "count": 1217605
        },
        {
          "label": "Domain",
          "value": "24",
          "count": 82938826
        },
        {
          "label": "Function",
          "value": "25",
          "count": 21750006
        },
        {
          "label": "Glycosylation",
          "value": "26",
          "count": 94780
        },
        {
          "label": "Helix",
          "value": "27",
          "count": 35254
        },
        {
          "label": "Induction",
          "value": "28",
          "count": 74633
        },
        {
          "label": "Initiator methionine",
          "value": "29",
          "count": 48392
        },
        {
          "label": "Intramembrane",
          "value": "30",
          "count": 4282
        },
        {
          "label": "Lipidation",
          "value": "31",
          "count": 51234
        },
        {
          "label": "Mass spectrometry",
          "value": "32",
          "count": 5991
        },
        {
          "label": "Modified residue",
          "value": "34",
          "count": 2025290
        },
        {
          "label": "Motif",
          "value": "35",
          "count": 711419
        },
        {
          "label": "Mutagenesis",
          "value": "36",
          "count": 21977
        },
        {
          "label": "Natural variant",
          "value": "37",
          "count": 17678
        },
        {
          "label": "Non-standard residue",
          "value": "38",
          "count": 8069
        },
        {
          "label": "Pathway",
          "value": "40",
          "count": 9178461
        },
        {
          "label": "Peptide",
          "value": "41",
          "count": 9482
        },
        {
          "label": "Pharmaceutical use",
          "value": "42",
          "count": 162
        },
        {
          "label": "Polymorphism",
          "value": "43",
          "count": 1393
        },
        {
          "label": "Propeptide",
          "value": "44",
          "count": 22279
        },
        {
          "label": "PTM comments",
          "value": "45",
          "count": 521882
        },
        {
          "label": "Region",
          "value": "46",
          "count": 44732017
        },
        {
          "label": "Repeat",
          "value": "47",
          "count": 2559088
        },
        {
          "label": "RNA editing",
          "value": "48",
          "count": 646
        },
        {
          "label": "Signal peptide",
          "value": "49",
          "count": 12088890
        },
        {
          "label": "Subcellular location",
          "value": "50",
          "count": 41464859
        },
        {
          "label": "Subunit structure",
          "value": "51",
          "count": 12469345
        },
        {
          "label": "Tissue specificity",
          "value": "52",
          "count": 51756
        },
        {
          "label": "Topological domain",
          "value": "53",
          "count": 82140
        },
        {
          "label": "Toxic dose",
          "value": "54",
          "count": 715
        },
        {
          "label": "Transit peptide",
          "value": "55",
          "count": 9928
        },
        {
          "label": "Transmembrane",
          "value": "56",
          "count": 27633314
        },
        {
          "label": "Turn",
          "value": "57",
          "count": 28426
        },
        {
          "label": "Zinc finger",
          "value": "58",
          "count": 375371
        }
      ]
    },
    {
      "label": "Protein existence",
      "name": "existence",
      "allowMultipleSelection": true,
      "values": [
        {
          "label": "Predicted",
          "value": "4",
          "count": 90824731
        },
        {
          "label": "Homology",
          "value": "3",
          "count": 57238708
        },
        {
          "label": "Transcript level",
          "value": "2",
          "count": 1334130
        },
        {
          "label": "Protein level",
          "value": "1",
          "count": 410841
        },
        {
          "label": "Uncertain",
          "value": "5",
          "count": 1729
        }
      ]
    },
    {
      "label": "Annotation score",
      "name": "annotation_score",
      "allowMultipleSelection": true,
      "values": [
        {
          "value": "5",
          "count": 226467
        },
        {
          "value": "4",
          "count": 758600
        },
        {
          "value": "3",
          "count": 8646463
        },
        {
          "value": "2",
          "count": 33713776
        },
        {
          "value": "1",
          "count": 106464833
        }
      ]
    },
    {
      "label": "Sequence length",
      "name": "length",
      "allowMultipleSelection": true,
      "values": [
        {
          "label": "1 - 200",
          "value": "[1 TO 200]",
          "count": 44869886
        },
        {
          "label": "201 - 400",
          "value": "[201 TO 400]",
          "count": 53923483
        },
        {
          "label": "401 - 600",
          "value": "[401 TO 600]",
          "count": 27647416
        },
        {
          "label": "601 - 800",
          "value": "[601 TO 800]",
          "count": 10787206
        },
        {
          "label": ">= 801",
          "value": "[801 TO *]",
          "count": 12582148
        }
      ]
    }
  ],
  "results": []
}

```

### DashboardKpisDto

can retrieve some data for DashboardKpisDto

```java 
public record DashboardKpisDto(long totalProteins, long reviewedCount, long unreviewedCount, int organismCount,
                               int taxonCount, int avgLength, long avgMolecularWeight, int minLength,
                               int maxLength) {
}

long reviewedCount = 575503;
long unreviewedCount = 149234636;

long totalProteins = reviewedCount + unreviewedCount; // 149810139


```

### endpoint B :  https://rest.uniprot.org/taxonomy/search?facets=superkingdom%2Ctaxonomies_with&query=*&size=0

return taxonomy data for DashboardKpisDto
organizme same as taxonCount

```json 
{
  "facets": [
    {
      "label": "Superkingdom",
      "name": "superkingdom",
      "allowMultipleSelection": false
    },
    {
      "label": "Taxons with",
      "name": "taxonomies_with",
      "allowMultipleSelection": true,
      "values": [
        {
          "label": "UniProtKB entries",
          "value": "1_uniprotkb",
          "count": 219400
        },
        {
          "label": "Reviewed (Swiss-Prot) entries",
          "value": "2_reviewed",
          "count": 14898
        },
        {
          "label": "Unreviewed (TrEMBL) entries",
          "value": "3_unreviewed",
          "count": 215136
        },
        {
          "label": "Reference proteomes",
          "value": "4_reference",
          "count": 35514
        },
        {
          "label": "Proteomes",
          "value": "5_proteome",
          "count": 174075
        }
      ]
    }
  ],
  "results": []
}
```

retrieve taxonCount from header x-total-results : 3141578
so taxonCount = 3141578

### endpoint C : https://rest.uniprot.org/uniprotkb/search?format=json&query=%28*%29&size=1&sort=length+asc

Minlength from this object

```json 
{
  "sequence": {
    "value": "EI",
    "length": 2
  }
}
```

### endpoint D :  https://rest.uniprot.org/uniprotkb/search?format=json&query=%28*%29&size=1&sort=length+desc

Maxlength from this object

```json
{
  "sequence": {
    "value": "ABCD...",
    "length": 46734
  }
}
```

a job to retrieve avgLength and avgMolecularWeight would require additional endpoints or calculations, as the provided
endpoints do not directly return these values. You may need to query the UniProt API for all protein entries and
calculate the average length and molecular weight based on the retrieved data.

### Pagination

to use it
run https://rest.uniprot.org/uniprotkb/search?fields=accession,reviewed,id,protein_name,gene_names,organism_name,length&query=organism_id%3A9606&&size=25
in the response a header "link" contains link to next page with cursor token
<https://rest.uniprot.org/uniprotkb/search?fields=accession,reviewed,id,protein_name,gene_names,organism_name,length&query=organism_id%3A9606&cursor=bkl0unsxhlbup27xg168lgjhxwwtgjy8otyew&size=25>;
rel="next"
call it and do it with next query until no cursor token for link exists it is the end
or api reach total result defined in response header x-total-results

## EvidenceDistributionDto

to collect it use endpoint A : the part with label "Protein existence"
convert value to integer for evidenceLevel

```json
{
  "label": "Protein existence",
  "name": "existence",
  "allowMultipleSelection": true,
  "values": [
    {
      "label": "Predicted",
      "value": "4",
      "count": 90824731
    },
    {
      "label": "Homology",
      "value": "3",
      "count": 57238708
    },
    {
      "label": "Transcript level",
      "value": "2",
      "count": 1334130
    },
    {
      "label": "Protein level",
      "value": "1",
      "count": 410841
    },
    {
      "label": "Uncertain",
      "value": "5",
      "count": 1729
    }
  ]
}
```

### KeywordFrequency

### endpoint E https://rest.uniprot.org/keywords/search?format=json&query=%28*%29&size=5

retrieve all keywords with statistics for reviewed/unreviewed ( sum is the 'count')

```json
 "statistics": {
"reviewedProteinCount": 2773,
"unreviewedProteinCount": 410675
}
```

for top keywords use endpoint A

```json
{
  "label": "Proteins with",
  "name": "proteins_with",
  "allowMultipleSelection": true,
  "values": [
    {
      "label": "3D structure",
      "value": "1",
      "count": 74161
    },
    {
      "label": "Active site",
      "value": "2",
      "count": 6822715
    }
  ]
}
```

### LengthHistogramBucketDto

use endpoint A section

```json
 {
  "label": "Sequence length",
  "name": "length",
  "allowMultipleSelection": true,
  "values": [
    {
      "label": "1 - 200",
      "value": "[1 TO 200]",
      "count": 44869886
    },
    {
      "label": "201 - 400",
      "value": "[201 TO 400]",
      "count": 53923483
    },
    {
      "label": "401 - 600",
      "value": "[401 TO 600]",
      "count": 27647416
    },
    {
      "label": "601 - 800",
      "value": "[601 TO 800]",
      "count": 10787206
    },
    {
      "label": ">= 801",
      "value": "[801 TO *]",
      "count": 12582148
    }
  ]
}
```

or use search endpoint with custom bucket
example : https://rest.uniprot.org/uniprotkb/search?format=json&query=%28*%29+AND+%28length%3A%5B1+TO+200%5D%29&size=1
and retrive 'count' from response header x-total-results

### OrganismCountDto

use this for detailed organisme one by one
https://rest.uniprot.org/taxonomy/search?format=json&query=%28%28tax_id%3A9606%29%29&size=1

```json
{
  "statistics": {
    "reviewedProteinCount": 20432,
    "unreviewedProteinCount": 190359,
    "referenceProteomeCount": 1,
    "proteomeCount": 2
  }
}
```

or top organism use endpoint A

```json
{
  "label": "Popular organisms",
  "name": "model_organism",
  "allowMultipleSelection": true,
  "values": [
    {
      "label": "Human",
      "value": "9606",
      "count": 210709
    },
    {
      "label": "Mouse",
      "value": "10090",
      "count": 87782
    }
  ]
}
```

