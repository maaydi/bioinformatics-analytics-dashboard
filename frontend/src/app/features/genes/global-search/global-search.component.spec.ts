import {ComponentFixture, TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {GlobalSearchComponent} from './global-search.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

describe('GlobalSearchComponent', () => {
  let component: GlobalSearchComponent;
  let fixture: ComponentFixture<GlobalSearchComponent>;

  beforeEach(async () => {
    vi.useFakeTimers();

    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reflect incoming globalSearch value in input signal', () => {
    fixture.componentRef.setInput('filters', {
      globalSearch: 'kinase'
    } satisfies GeneFilterSnapshot);
    fixture.detectChanges();

    expect(component.globalSearchValue()).toBe('kinase');
  });

  it('should emit filterChange with merged filters after debounce', () => {
    const emitSpy = vi.fn();
    component.filterChange.subscribe(emitSpy);

    fixture.componentRef.setInput('filters', {
      accession: 'P12345',
      reviewed: true
    } satisfies GeneFilterSnapshot);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'abc';
    input.dispatchEvent(new Event('input'));

    vi.advanceTimersByTime(299);
    expect(emitSpy).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(emitSpy).toHaveBeenCalledOnce();
    expect(emitSpy).toHaveBeenCalledWith({
      accession: 'P12345',
      reviewed: true,
      globalSearch: 'abc',
    });
  });

  it('should emit only once for fast consecutive typing', () => {
    const emitSpy = vi.fn();
    component.filterChange.subscribe(emitSpy);

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;

    input.value = 'k';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(100);

    input.value = 'ki';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(100);

    input.value = 'kinase';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(300);

    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledWith({globalSearch: 'kinase'});
  });
});

