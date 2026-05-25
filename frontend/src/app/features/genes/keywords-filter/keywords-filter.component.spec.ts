import {ComponentFixture, TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {Observable, of, throwError} from 'rxjs';
import {FormControl} from '@angular/forms';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {KeywordsFilterComponent} from './keywords-filter.component';
import {GenesService} from '@features/genes/genes.service';

type KeywordsFilterHarness = {
  selectedKeywords: { (): string[]; set: (value: string[]) => void };
  keywordSearchCtrl: FormControl<string>;
  filteredKeywords$: Observable<string[]>;
  toggleKeyword: (event: MatAutocompleteSelectedEvent) => void;
  getSelectedKeywords: () => string;
  onTouched: () => void;
};

describe('KeywordsFilterComponent', () => {
  let component: KeywordsFilterComponent;
  let fixture: ComponentFixture<KeywordsFilterComponent>;
  let genesServiceMock: { loadKeywords: ReturnType<typeof vi.fn> };

  const asHarness = (): KeywordsFilterHarness => component as unknown as KeywordsFilterHarness;

  beforeEach(async () => {
    genesServiceMock = {
      loadKeywords: vi.fn().mockReturnValue(of(['Kinase', 'Receptor', 'Ligase']))
    };

    await TestBed.configureTestingModule({
      imports: [KeywordsFilterComponent],
      providers: [{provide: GenesService, useValue: genesServiceMock}]
    }).compileComponents();

    fixture = TestBed.createComponent(KeywordsFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should create and load keywords on init', () => {
    expect(component).toBeTruthy();
    expect(genesServiceMock.loadKeywords).toHaveBeenCalledOnce();
  });

  it('should write value from parent and expose compact selected summary', () => {
    const harness = asHarness();

    component.writeValue(['Kinase', 'Ligase']);

    expect(harness.selectedKeywords()).toEqual(['Kinase', 'Ligase']);
    expect(harness.getSelectedKeywords()).toBe('2 Selected');
  });

  it('should enable and disable the internal input control', () => {
    const harness = asHarness();

    component.setDisabledState(true);
    expect(harness.keywordSearchCtrl.disabled).toBe(true);

    component.setDisabledState(false);
    expect(harness.keywordSearchCtrl.disabled).toBe(false);
  });

  it('should filter loaded keywords based on debounced search text', () => {
    vi.useFakeTimers();
    const harness = asHarness();
    let latestKeywords: string[] = [];
    const subscription = harness.filteredKeywords$.subscribe((value) => {
      latestKeywords = value as unknown as string[];
    });

    harness.keywordSearchCtrl.setValue('kin');
    vi.advanceTimersByTime(200);

    expect(latestKeywords).toEqual(['Kinase']);
    subscription.unsubscribe();
  });

  it('should add and remove keyword selections and notify parent callbacks', () => {
    vi.useFakeTimers();
    const harness = asHarness();

    const onChange = vi.fn();
    const onTouched = vi.fn();
    component.registerOnChange(onChange);
    component.registerOnTouched(onTouched);

    const deselect = vi.fn();
    const event = {option: {value: 'Kinase', deselect}} as unknown as MatAutocompleteSelectedEvent;

    harness.toggleKeyword(event);
    expect(harness.selectedKeywords()).toEqual(['Kinase']);
    expect(onChange).toHaveBeenCalledWith(['Kinase']);
    expect(onTouched).toHaveBeenCalledTimes(1);

    harness.toggleKeyword(event);
    expect(harness.selectedKeywords()).toEqual([]);
    expect(onChange).toHaveBeenLastCalledWith([]);
    expect(onTouched).toHaveBeenCalledTimes(2);

    vi.runAllTimers();
    expect(deselect).toHaveBeenCalledTimes(2);
  });

  it('should recover with an empty keyword set when backend loading fails', () => {
    vi.useFakeTimers();

    genesServiceMock.loadKeywords.mockReturnValueOnce(
      throwError(() => new Error('backend unavailable'))
    );

    const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {
    });

    const localFixture = TestBed.createComponent(KeywordsFilterComponent);
    localFixture.detectChanges();

    const harness = localFixture.componentInstance as unknown as KeywordsFilterHarness;
    let latestKeywords: string[] = [];
    const subscription = harness.filteredKeywords$.subscribe((value) => {
      latestKeywords = value as unknown as string[];
    });

    harness.keywordSearchCtrl.setValue('kin');
    vi.advanceTimersByTime(200);

    expect(latestKeywords).toEqual([]);
    expect(consoleSpy).toHaveBeenCalled();

    subscription.unsubscribe();
    consoleSpy.mockRestore();
  });
});

