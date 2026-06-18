import {inject, Injectable} from '@angular/core';
import {MatSnackBar, MatSnackBarConfig, MatSnackBarRef, TextOnlySnackBar} from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  private readonly defaultConfig: MatSnackBarConfig = {
    duration: 3000,
    horizontalPosition: 'end',
    verticalPosition: 'top',
  };

  /**
   * Show a generic message
   */
  show(message: string, action = 'OK', config?: MatSnackBarConfig): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, {...this.defaultConfig, ...config});
  }

  /**
   * Show a success toast with specific styling
   */
  success(message: string, action = 'Close'): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, {
      ...this.defaultConfig,
      panelClass: ['success-snackbar'],
    });
  }

  /**
   * Show an error toast with specific styling
   */
  error(message: string, action = 'Dismiss'): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, {
      ...this.defaultConfig,
      panelClass: ['error-snackbar'],
    });
  }
}
