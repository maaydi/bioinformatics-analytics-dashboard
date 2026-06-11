import {inject, Injectable} from '@angular/core';
import html2canvas from 'html2canvas';
import {ECharts} from 'echarts';
import {NotificationService} from '@shared/directive/notification.service';

@Injectable({
  providedIn: 'root'
})
export class ImageExportService {

  private readonly notify = inject(NotificationService);

  public async exportElement(element: HTMLElement, fileName: string, hideSelector?: string) {
    let hiddenElements: HTMLElement[] = [];

    if (hideSelector) {
      hiddenElements = Array.from(element.querySelectorAll<HTMLElement>(hideSelector));
      hiddenElements.forEach(el => (el.style.visibility = 'hidden'));
    }
    const toastRef = this.notify.show(`Start exporting "${fileName} PNG"`, 'OK', {
      duration: 10000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['success-snackbar']
    });
    try {
      await new Promise(resolve => setTimeout(resolve, 50));
      const canvas = await html2canvas(element, {
        scale: 2,
        backgroundColor: null,
        logging: false,
        useCORS: true
      });

      const dataUrl = canvas.toDataURL('image/png');

      const link = document.createElement('a');
      link.download = `${fileName}.png`;
      link.href = dataUrl;
      link.click();
      link.remove();

    } catch (error) {
      console.error('Failed to export image:', error);
    } finally {
      hiddenElements.forEach(el => (el.style.visibility = 'visible'));
      toastRef.dismiss();
    }
  }

  public async exportEChart(parent: HTMLElement, chart: ECharts, fileName: string, hideSelector?: string) {
    let hiddenElements: HTMLElement[] = [];

    if (hideSelector) {
      hiddenElements = Array.from(parent.querySelectorAll<HTMLElement>(hideSelector));
      hiddenElements.forEach(el => (el.style.visibility = 'hidden'));
    }
    try {
      const dataUrl = chart.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#ffffff'
      });

      const link = document.createElement('a');
      link.download = `${fileName}.png`;
      link.href = dataUrl;
      link.click();
      link.remove();

    } catch (error) {
      console.error('Failed to export image:', error);
    } finally {
      hiddenElements.forEach(el => (el.style.visibility = 'visible'));
    }
  }

}
