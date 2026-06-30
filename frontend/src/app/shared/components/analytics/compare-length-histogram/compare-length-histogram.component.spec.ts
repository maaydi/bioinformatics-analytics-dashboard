import {ComponentFixture, TestBed} from '@angular/core/testing';
import {CompareLengthHistogramComponent} from './compare-length-histogram.component';
import {ImageExportService} from '@shared/directive/image-export-service';
import {LengthHistogramBucket} from '@core/models/analytics.model';

describe('CompareLengthHistogramComponent', () => {
  let component: CompareLengthHistogramComponent;
  let fixture: ComponentFixture<CompareLengthHistogramComponent>;
  let mockImageExportService: { exportElement: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockImageExportService = {
      exportElement: vi.fn().mockResolvedValue(undefined)
    };

    await TestBed.configureTestingModule({
      imports: [CompareLengthHistogramComponent],
      providers: [
        {provide: ImageExportService, useValue: mockImageExportService}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompareLengthHistogramComponent);
    component = fixture.componentInstance;

    // Set mandatory inputs
    fixture.componentRef.setInput('bucketsA', []);
    fixture.componentRef.setInput('bucketsB', []);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should correctly process and merge viewBuckets', () => {
    const bucketsA: LengthHistogramBucket[] = [
      {bucket: 1, rangeMin: 0, rangeMax: 50, count: 10},
      {bucket: 2, rangeMin: 50, rangeMax: 100, count: 20}
    ];
    const bucketsB: LengthHistogramBucket[] = [
      {bucket: 2, rangeMin: 50, rangeMax: 100, count: 5},
      {bucket: 3, rangeMin: 100, rangeMax: 150, count: 15}
    ];

    fixture.componentRef.setInput('bucketsA', bucketsA);
    fixture.componentRef.setInput('bucketsB', bucketsB);
    fixture.detectChanges();

    const merged = component['viewBuckets']();
    expect(merged.length).toBe(3);

    expect(merged[0]).toEqual({
      rangeLabel: '0-50',
      rangeMin: 0,
      rangeMax: 50,
      countA: 10,
      countB: 0
    });

    expect(merged[1]).toEqual({
      rangeLabel: '50-100',
      rangeMin: 50,
      rangeMax: 100,
      countA: 20,
      countB: 5
    });

    expect(merged[2]).toEqual({
      rangeLabel: '100-150',
      rangeMin: 100,
      rangeMax: 150,
      countA: 0,
      countB: 15
    });
  });

  it('should calculate maxCount and totals correctly', () => {
    fixture.componentRef.setInput('bucketsA', [{rangeMin: 0, rangeMax: 50, count: 10}]);
    fixture.componentRef.setInput('bucketsB', [{rangeMin: 0, rangeMax: 50, count: 25}]);
    fixture.detectChanges();

    expect(component['totalCountA']()).toBe(10);
    expect(component['totalCountB']()).toBe(25);
    expect(component['maxCount']()).toBe(25);
  });

  it('should compute bar stats correctly', () => {
    fixture.componentRef.setInput('bucketsA', [{bucket: 1, rangeMin: 0, rangeMax: 10, count: 40}]);
    fixture.componentRef.setInput('bucketsB', [{bucket: 2, rangeMin: 10, rangeMax: 20, count: 50}]);
    fixture.detectChanges();

    expect(component['barHeight'](25)).toBe(50); // 25 is 50% of max 50
    expect(component['barShareA'](10)).toBe(25); // 10 is 25% of total 40
    expect(component['barShareB'](10)).toBe(20); // 10 is 20% of total 50
  });

  it('should emit rangeSelected on bucket selection', () => {
    const emitSpy = vi.spyOn(component.rangeSelected, 'emit');
    component['selectLengthRange']({
      rangeLabel: '10-20',
      rangeMin: 10,
      rangeMax: 20,
      countA: 1,
      countB: 1
    });

    expect(emitSpy).toHaveBeenCalledWith({min: 10, max: 20});
  });

  it('should call ImageExportService on exportAsImage', async () => {
    // The ViewChild is required
    const div = document.createElement('div');
    component.chartCard = {nativeElement: div} as any;

    await component['exportAsImage']();

    expect(mockImageExportService.exportElement).toHaveBeenCalledWith(
      div,
      'compare-length-histogram',
      '.no-export'
    );
  });
});

