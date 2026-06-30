import {TestBed} from '@angular/core/testing';
import {NotificationService} from './notification.service';
import {MatSnackBar} from '@angular/material/snack-bar';

describe('NotificationService', () => {
  let service: NotificationService;
  let mockSnackBar: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    mockSnackBar = {
      open: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        {provide: MatSnackBar, useValue: mockSnackBar}
      ]
    });
    service = TestBed.inject(NotificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should open generic snackbar', () => {
    service.show('test message', 'OK', {panelClass: ['custom']});
    expect(mockSnackBar.open).toHaveBeenCalledWith('test message', 'OK', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['custom']
    });
  });

  it('should open success snackbar', () => {
    service.success('success message');
    expect(mockSnackBar.open).toHaveBeenCalledWith('success message', 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['success-snackbar']
    });
  });

  it('should open error snackbar', () => {
    service.error('error message');
    expect(mockSnackBar.open).toHaveBeenCalledWith('error message', 'Dismiss', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['error-snackbar']
    });
  });

  it('should open warning snackbar with custom duration', () => {
    service.warning('warning message', 'Action', 6000);
    expect(mockSnackBar.open).toHaveBeenCalledWith('warning message', 'Action', {
      duration: 6000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['warning-snackbar']
    });
  });
});

