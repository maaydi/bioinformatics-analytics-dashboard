package com.bioinformatics.dashboard.providers.uniprotkb.dto;

/**
 * A lightweight representation of a protein feature annotation.
 *
 * <p>Contains only the feature type (e.g., "active_site", "binding_site", "transmembrane"),
 * omitting detailed positional, evidential, or description information. Suitable for
 * quick overview displays and filtering.</p>
 *
 * @param type the feature type code or name (e.g., "ft_act_site")
 */
public record FeatureLight(String type) {
}
