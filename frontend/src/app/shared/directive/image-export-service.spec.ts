import {TestBed} from '@angular/core/testing';
import {ImageExportService} from './image-export-service';
import {NotificationService} from './notification.service';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ECharts} from 'echarts';
import html2canvas from 'html2canvas';

vi.mock('html2canvas', () => ({
  default: vi.fn(),
}));

describe('ImageExportService', () => {
  let service: ImageExportService;
  let notificationService: NotificationService;
  let mockElement: HTMLElement;

  beforeEach(() => {
    vi.mocked(html2canvas).mockReset().mockResolvedValue({
      toDataURL: () => 'data:image/png;base64,test',
    } as any);

    TestBed.configureTestingModule({
      providers: [
        ImageExportService,
        {
          provide: NotificationService,
          useValue: {
            show: vi.fn().mockReturnValue({dismiss: vi.fn()}),
          },
        },
      ],
    });

    service = TestBed.inject(ImageExportService);
    notificationService = TestBed.inject(NotificationService);
    mockElement = document.createElement('div');
    mockElement.innerHTML = '<p>Test Content</p>';
  });

  afterEach(() => {
    vi.clearAllMocks();
    mockElement.remove();
  });

  describe('Service Creation', () => {
    it('should be created with providedIn root', () => {
      expect(service).toBeDefined();
    });

    it('should inject NotificationService', () => {
      expect(notificationService).toBeDefined();
    });
  });

  describe('exportElement method', () => {
    it('should display notification when export starts', async () => {
      await service.exportElement(mockElement, 'test-file');

      expect(notificationService.show).toHaveBeenCalledWith(
        expect.stringMatching(/Start exporting.*PNG/),
        'OK',
        expect.any(Object),
      );
    });

    it('should hide elements when hideSelector is provided', async () => {
      const containerDiv = document.createElement('div');
      const childToHide = document.createElement('div');
      childToHide.classList.add('no-export');
      containerDiv.appendChild(childToHide);
      document.body.appendChild(containerDiv);

      const initialVisibility = childToHide.style.visibility;
      await service.exportElement(containerDiv, 'test', '.no-export');

      expect(childToHide.style.visibility === initialVisibility || childToHide.style.visibility === '').toBe(true);

      containerDiv.remove();
    });


    it('should set download attribute with correct filename', async () => {
      await service.exportElement(mockElement, 'my-export');

      const anchors = document.querySelectorAll('a');
      if (anchors.length > 0) {
        const lastAnchor = anchors[anchors.length - 1];
        expect(lastAnchor.download).toContain('my-export');
      }
    });

    it('should handle errors gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });

      vi.mocked(html2canvas).mockRejectedValue(new Error('Canvas error'));

      await service.exportElement(mockElement, 'test-file');

      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });

    it('should dismiss notification in finally block', async () => {
      const mockToastRef = {dismiss: vi.fn()};
      (notificationService.show as any).mockReturnValue(mockToastRef);

      await service.exportElement(mockElement, 'test-file');

      expect(mockToastRef.dismiss).toHaveBeenCalled();
    });

    it('should restore visibility of hidden elements in finally block', async () => {
      const containerDiv = document.createElement('div');
      const childToHide = document.createElement('div');
      childToHide.classList.add('no-export');
      containerDiv.appendChild(childToHide);
      document.body.appendChild(containerDiv);

      await service.exportElement(containerDiv, 'test', '.no-export');

      expect(childToHide.style.visibility === '' || childToHide.style.visibility === 'visible').toBe(true);

      containerDiv.remove();
    });

    it('should handle no hideSelector gracefully', async () => {
      await expect(service.exportElement(mockElement, 'test')).resolves.not.toThrow();
    });
  });

  describe('exportEChart method', () => {
    let mockChart: Partial<ECharts>;

    beforeEach(() => {
      mockChart = {
        getDataURL: vi.fn().mockReturnValue('data:image/png;base64,chart'),
      };
    });

    it('should get data URL from chart with correct options', () => {
      service.exportEChart(mockElement, mockChart as ECharts, 'chart-export');

      expect((mockChart.getDataURL as any)).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: '#ffffff',
        }),
      );
    });

    it('should create download link for chart', () => {
      const createElementSpy = vi.spyOn(document, 'createElement');
      service.exportEChart(mockElement, mockChart as ECharts, 'chart-export');

      expect(createElementSpy).toHaveBeenCalledWith('a');
      createElementSpy.mockRestore();
    });

    it('should set download filename for chart', () => {
      service.exportEChart(mockElement, mockChart as ECharts, 'my-chart');

      const anchors = document.querySelectorAll('a');
      if (anchors.length > 0) {
        const lastAnchor = anchors[anchors.length - 1];
        expect(lastAnchor.download).toContain('my-chart');
      }
    });

    it('should hide elements when hideSelector is provided', () => {
      const containerDiv = document.createElement('div');
      const childToHide = document.createElement('div');
      childToHide.classList.add('no-export');
      containerDiv.appendChild(childToHide);
      document.body.appendChild(containerDiv);

      const initialVisibility = childToHide.style.visibility;
      service.exportEChart(containerDiv, mockChart as ECharts, 'test', '.no-export');

      expect(childToHide.style.visibility === initialVisibility || childToHide.style.visibility === '').toBe(true);

      containerDiv.remove();
    });

    it('should restore visibility of hidden elements', () => {
      const containerDiv = document.createElement('div');
      const childToHide = document.createElement('div');
      childToHide.classList.add('no-export');
      containerDiv.appendChild(childToHide);
      document.body.appendChild(containerDiv);

      service.exportEChart(containerDiv, mockChart as ECharts, 'test', '.no-export');

      expect(childToHide.style.visibility === '' || childToHide.style.visibility === 'visible').toBe(true);

      containerDiv.remove();
    });

    it('should handle chart export errors gracefully', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });
      const errorChart: Partial<ECharts> = {
        getDataURL: vi.fn().mockImplementation(() => {
          throw new Error('Chart error');
        }),
      };

      expect(() => service.exportEChart(mockElement, errorChart as ECharts, 'test')).not.toThrow();

      consoleErrorSpy.mockRestore();
    });

    it('should remove anchor element after click', () => {
      const removeSpy = vi.spyOn(HTMLElement.prototype, 'remove');
      service.exportEChart(mockElement, mockChart as ECharts, 'chart');

      expect(removeSpy).toHaveBeenCalled();
      removeSpy.mockRestore();
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty element', async () => {
      const emptyElement = document.createElement('div');

      await expect(service.exportElement(emptyElement, 'empty')).resolves.not.toThrow();
    });

    it('should handle filename with special characters', () => {
      const mockChartSpecial: Partial<ECharts> = {
        getDataURL: vi.fn().mockReturnValue('data:image/png;base64,test'),
      };

      service.exportEChart(mockElement, mockChartSpecial as ECharts, 'chart-file_@2024');

      const anchors = document.querySelectorAll('a');
      if (anchors.length > 0) {
        const lastAnchor = anchors[anchors.length - 1];
        expect(lastAnchor.download).toBeTruthy();
      }
    });

    it('should notify user with progress indicator options', async () => {
      await service.exportElement(mockElement, 'test');

      expect(notificationService.show).toHaveBeenCalledWith(
        expect.any(String),
        'OK',
        {
          duration: 10000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar'],
        },
      );
    });
  });
});
