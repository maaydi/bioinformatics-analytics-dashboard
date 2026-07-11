package com.bioinformatics.dashboard.providers.uniprotkb.dto;


import java.util.List;

public record UniprotKbResponse<T>(List<T> results) {
}
