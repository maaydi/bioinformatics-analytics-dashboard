package com.bioinformatics.common.uniprot.dto;


import java.util.List;

public record UniprotKbResponse<T>(List<T> results) {
}
