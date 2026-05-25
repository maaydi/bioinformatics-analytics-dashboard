import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

import {ActiveFiltersComponent} from './active-filters.component';

describe('ActiveFiltersComponent', () => {
  let component: ActiveFiltersComponent;
  let fixture: ComponentFixture<ActiveFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActiveFiltersComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return no chips when filters input is null', () => {
    fixture.componentRef.setInput('filters', null);
    fixture.detectChanges();

    expect(component.filtersChips()).toEqual([]);
  });

  it('should build chips for non-empty scalar and array filters', () => {
    const filters: GeneFilterSnapshot = {
      globalSearch: 'kinase',
      reviewed: true,
      taxid: 9606,
      evidenceLevels: [1, 2],
      keywords: ['membrane', 'transport']
    };

    fixture.componentRef.setInput('filters', filters);
    fixture.detectChanges();

    const chips = component.filtersChips();
    expect(chips.some((chip) => chip.label === 'Search' && chip.value === 'kinase')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Reviewed' && chip.value === 'Yes')).toBe(true);
    expect(chips.some((chip) => chip.label === 'TaxID' && chip.value === '9606')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Evidence' && chip.value.includes('1'))).toBe(true);
    expect(chips.some((chip) => chip.label === 'Keywords' && chip.value.includes('membrane'))).toBe(true);
  });

  it('should map reviewed=false to No and keep zero values', () => {
    const filters: GeneFilterSnapshot = {
      reviewed: false,
      taxid: 0,
      lengthMin: 0
    };

    fixture.componentRef.setInput('filters', filters);
    fixture.detectChanges();

    const chips = component.filtersChips();
    expect(chips.some((chip) => chip.label === 'Reviewed' && chip.value === 'No')).toBe(true);
    expect(chips.some((chip) => chip.label === 'TaxID' && chip.value === '0')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Length Min' && chip.value === '0')).toBe(true);
  });

  it('should emit filterRemoved when removeFilter is called', async () => {
    let removedKey: keyof GeneFilterSnapshot | undefined;
    component.filterRemoved.subscribe((key) => {
      removedKey = key;
    });

    component.removeFilter('accession');

    fixture.detectChanges();
    await fixture.whenStable();
    expect(removedKey).toBe('accession');
  });

  it('should emit chips count based on generated chips', async () => {
    const countSpy = vi.fn();
    component.setChipsCount.subscribe(countSpy);

    fixture.componentRef.setInput('filters', {
      accession: 'P12345',
      keywords: ['kinase']
    } satisfies GeneFilterSnapshot);
    fixture.detectChanges();

    component.filtersChips();

    await fixture.whenStable();
    expect(countSpy).toHaveBeenCalled();
    expect(countSpy).toHaveBeenLastCalledWith(2);
  });
});
