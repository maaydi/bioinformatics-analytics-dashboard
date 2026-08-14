package com.bioinformatics.dashboard.model.uniprot.dto;


import java.util.List;

public record UniprotKbResponse<T>(List<T> results) {
}
