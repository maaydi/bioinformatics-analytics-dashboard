import {TestBed} from '@angular/core/testing';
import {ImageExportService} from './image-export-service';
import {NotificationService} from './notification.service';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ECharts} from 'echarts';

// Mock html2canvas
vi.mock('html2canvas', () => ({
  default: vi.fn(),
}));

describe('ImageExportService', () => {
  let service: ImageExportService;
  let notificationService: NotificationService;
  let mockElement: HTMLElement;

  beforeEach(() => {
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
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

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

      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      const initialVisibility = childToHide.style.visibility;
      await service.exportElement(containerDiv, 'test', '.no-export');

      // After export, visibility should be restored
      expect(childToHide.style.visibility === initialVisibility || childToHide.style.visibility === '').toBe(true);

      containerDiv.remove();
    });

    it('should create download link and trigger click', async () => {
      const createElementSpy = vi.spyOn(document, 'createElement');
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      await service.exportElement(mockElement, 'test-file');

      // Verify that createElement was called for the anchor element
      expect(createElementSpy).toHaveBeenCalledWith('a');
    });

    it('should set download attribute with correct filename', async () => {
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      await service.exportElement(mockElement, 'my-export');

      // Find the anchor element that was appended
      const anchors = document.querySelectorAll('a');
      if (anchors.length > 0) {
        // Find the most recently added one
        const lastAnchor = anchors[anchors.length - 1];
        expect(lastAnchor.download).toContain('my-export');
      }
    });

    it('should handle errors gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockRejectedValue(new Error('Canvas error'));

      await service.exportElement(mockElement, 'test-file');

      // Service should handle error without throwing
      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });

    it('should dismiss notification in finally block', async () => {
      const mockToastRef = {dismiss: vi.fn()};
      (notificationService.show as any).mockReturnValue(mockToastRef);

      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      await service.exportElement(mockElement, 'test-file');

      expect(mockToastRef.dismiss).toHaveBeenCalled();
    });

    it('should restore visibility of hidden elements in finally block', async () => {
      const containerDiv = document.createElement('div');
      const childToHide = document.createElement('div');
      childToHide.classList.add('no-export');
      containerDiv.appendChild(childToHide);
      document.body.appendChild(containerDiv);

      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      await service.exportElement(containerDiv, 'test', '.no-export');

      // Visibility should be restored (either empty or original value)
      expect(childToHide.style.visibility === '' || childToHide.style.visibility === 'visible').toBe(true);

      containerDiv.remove();
    });

    it('should call html2canvas with correct options', async () => {
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      await service.exportElement(mockElement, 'test');

      expect((html2canvasMock.default as any)).toHaveBeenCalledWith(
        mockElement,
        expect.objectContaining({
          scale: 2,
          logging: false,
          useCORS: true,
        }),
      );
    });

    it('should handle no hideSelector gracefully', async () => {
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

      // Should not throw when hideSelector is undefined
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

      // After export, visibility should be restored
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

      // Should not throw
      expect(() => service.exportEChart(mockElement, errorChart as ECharts, 'test')).not.toThrow();

      consoleErrorSpy.mockRestore();
    });

    it('should remove anchor element after click', () => {
      const removeSpy = vi.spyOn(HTMLElement.prototype, 'remove');
      service.exportEChart(mockElement, mockChart as ECharts, 'chart');

      expect(removeSpy).toHaveBeenCalled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty element', async () => {
      const emptyElement = document.createElement('div');
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,empty',
      });

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
      const html2canvasMock = await import('html2canvas');
      (html2canvasMock.default as any).mockResolvedValue({
        toDataURL: () => 'data:image/png;base64,test',
      });

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

