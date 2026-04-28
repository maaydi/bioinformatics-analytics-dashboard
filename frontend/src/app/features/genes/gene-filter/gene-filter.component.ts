import { Component, Output, EventEmitter } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { GeneFilterSnapshot } from '../../../core/models/saved-filter.model';

/**
 * Presentational (dumb) filter panel component.
 *
 * Filter fields (documentation/overview.md §6 + api-contract.md §1):
 *   globalSearch, accession, geneNamePrimary, proteinFullName, reviewed (toggle),
 *   organism, taxid, lengthMin, lengthMax, molecularWeightMin, molecularWeightMax,
 *   evidenceLevels (multi-select), keywords, goTermId, goAspect, featureType
 *
 * Validation (documentation/validation-rules.md §2):
 *   - lengthMin ≤ lengthMax enforced client-side (cross-field validator)
 *   - goTermId pattern GO:\d{7}
 *   - No API request on invalid form
 *
 * @Output filterChange — emits the current filter snapshot when applied
 * @Output filterClear  — emits when "Clear All" is clicked
 *
 * TODO: implement in ticket GENE-002
 */
@Component({
  selector: 'app-gene-filter',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<form><!-- TODO: filter fields --></form>`,
})
export class GeneFilterComponent {
  @Output() filterChange = new EventEmitter<GeneFilterSnapshot>();
  @Output() filterClear  = new EventEmitter<void>();
}
