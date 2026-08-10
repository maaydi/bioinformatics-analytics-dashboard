import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {PageEvent} from '@angular/material/paginator';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';

import {ResultHeaderComponent} from './result-header.component';

describe('ResultHeaderComponent', () => {
  let component: ResultHeaderComponent;
  let fixture: ComponentFixture<ResultHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResultHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ResultHeaderComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose default computed values when data input is null', () => {
    expect(component.totalGenes()).toBe(0);
    expect(component.pageSize()).toBe(50);
    expect(component.pageIndex()).toBe(0);
  });

  it('should compute values from paged response input', () => {
    const data: PagedResponse<ProteinSummary> = {
      content: [],
      page: 3,
      size: 100,
      totalElements: 450,
      totalPages: 5
    };

    fixture.componentRef.setInput('data', data);
    fixture.detectChanges();

    expect(component.totalGenes()).toBe(450);
    expect(component.pageSize()).toBe(100);
    expect(component.pageIndex()).toBe(3);
  });

  it('should emit updatePage when paginator event occurs', () => {
    const emitSpy = vi.fn();
    component.updatePage.subscribe(emitSpy);

    const event: PageEvent = {
      previousPageIndex: 0,
      pageIndex: 2,
      pageSize: 200,
      length: 450
    };

    component.onPageChange(event);

    expect(emitSpy).toHaveBeenCalledOnce();
    expect(emitSpy).toHaveBeenCalledWith({page: 0, size: 200});
  });
});
