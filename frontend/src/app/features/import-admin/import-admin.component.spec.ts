import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ImportAdminComponent} from './import-admin.component';
import {ImportAdminService} from './import-admin.service';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatTableModule} from '@angular/material/table';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {ImportJobSummary} from '@core/models/import.model';
import {vi} from 'vitest';
import {provideHttpClient} from '@angular/common/http';

describe('ImportAdminComponent', () => {
  let component: ImportAdminComponent;
  let fixture: ComponentFixture<ImportAdminComponent>;
  let importService: ImportAdminService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ImportAdminComponent,

        MatPaginatorModule,
        MatTableModule,
        MatProgressBarModule,
        MatCardModule,
        MatFormFieldModule,
        MatSelectModule,
        FormsModule,
        MatButtonModule,
        MatIconModule,
      ],
      providers: [ImportAdminService, provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    importService = TestBed.inject(ImportAdminService);
    fixture = TestBed.createComponent(ImportAdminComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // OnPush strategy is implicit in Angular 21+ standalone; skipping explicit test

  it('should initialize signals with default values', () => {
    expect(component.selectedFile()).toBeNull();
    expect(component.strategy()).toBe('OVERWRITE');
    expect(component.isUploading()).toBe(false);
    expect(component.currentProgress()).toBe(0);
    expect(component.errorMessage()).toBeNull();
    expect(component.jobHistory()).toEqual([]);
    expect(component.totalJobs()).toBe(0);
    expect(component.pageSize()).toBe(5);
    expect(component.pageIndex()).toBe(0);
  });

  it('should cleanup subscriptions on destroy', () => {
    const stopPollingSpy = vi.spyOn(component as any, 'stopPolling');
    const stopLoadHistorySpy = vi.spyOn(component as any, 'stopLoadHistory');

    component.ngOnDestroy();

    expect(stopPollingSpy).toHaveBeenCalled();
    expect(stopLoadHistorySpy).toHaveBeenCalled();
  });

  describe('File Selection', () => {
    it('should accept valid .dat file', () => {
      const file = new File(['content'], 'test.dat', {type: 'text/plain'});
      const event = {
        target: {
          files: [file],
        } as any,
      } as Event;

      component.onFileSelected(event);

      expect(component.selectedFile()).toBe(file);
      expect(component.errorMessage()).toBeNull();
    });

    it('should accept valid .tsv file', () => {
      const file = new File(['content'], 'test.tsv', {type: 'text/tab-separated-values'});
      const event = {
        target: {
          files: [file],
        } as any,
      } as Event;

      component.onFileSelected(event);

      expect(component.selectedFile()).toBe(file);
      expect(component.errorMessage()).toBeNull();
    });

    it('should reject file with invalid extension', () => {
      const file = new File(['content'], 'test.pdf', {type: 'application/pdf'});
      const event = {
        target: {
          files: [file],
          value: '',
        } as any,
      } as Event;

      component.onFileSelected(event);

      expect(component.selectedFile()).toBeNull();
      expect(component.errorMessage()).toContain('Unsupported file type');
    });
  });

  describe('Import Submission', () => {
    it('should not submit if no file selected', () => {
      const triggerSpy = vi.spyOn(importService, 'triggerImport');
      component.selectedFile.set(null);

      component.submitImport();

      expect(triggerSpy).not.toHaveBeenCalled();
    });
  });

  describe('Pagination', () => {
    it('should handle pagination change', () => {
      const pageEvent: PageEvent = {
        pageIndex: 1,
        pageSize: 10,
        length: 50,
      };

      const loadHistorySpy = vi.spyOn(component as any, 'loadJobHistory');

      component.onPageChange(pageEvent);

      expect(component.pageIndex()).toBe(1);
      expect(component.pageSize()).toBe(10);
      expect(component.forceLoadHistory()).toBe(true);
      expect(loadHistorySpy).toHaveBeenCalled();
    });
  });

  describe('Job History', () => {
    it('should display job history in table', () => {
      const mockJobs: ImportJobSummary[] = [
        {
          id: 'job-1',
          status: 'COMPLETED',
          fileName: 'proteins.dat',
          progressPercent: 100,
          entryCount: 1000,
          durationMs: 5000,
          createdAt: new Date().toISOString(),
          completedAt: new Date().toISOString(),
          errorMessage: null,
        },
      ];

      component.jobHistory.set(mockJobs);
      component.totalJobs.set(1);

      expect(component.jobHistory().length).toBe(1);
      expect(component.totalJobs()).toBe(1);
    });
  });

  describe('File Input Trigger', () => {
    it('should trigger file input click', () => {
      const mockFileInput = document.createElement('input');
      mockFileInput.type = 'file';
      const clickSpy = vi.spyOn(mockFileInput, 'click');

      component.fileInput = {nativeElement: mockFileInput} as any;

      component.triggerFileInput();

      expect(clickSpy).toHaveBeenCalled();
    });
  });
});

