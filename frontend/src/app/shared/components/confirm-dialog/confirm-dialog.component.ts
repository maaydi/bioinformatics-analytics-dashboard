import {Component, inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';

/**
 * Reusable confirmation dialog data contract.
 * Pass to MatDialog.open(ConfirmDialogComponent, { data: DialogData })
 */
export interface DialogData {
  /** Dialog title */
  title: string;
  /** Confirmation prompt message */
  message: string;
  /** Label for confirm button (e.g., "Delete") */
  confirmLabel: string;
  /** Label for cancel button */
  cancelLabel: string;
}

/**
 * ConfirmDialogComponent — Generic confirmation modal.
 *
 * Usage:
 * ```
 * const dialogRef = this.dialog.open(ConfirmDialogComponent, {
 *   data: { title: '...', message: '...', confirmLabel: '...', cancelLabel: '...' }
 * });
 * dialogRef.afterClosed().subscribe(confirmed => { ... });
 * ```
 *
 * - Returns true if user confirmed
 * - Returns false if user cancelled or dismissed
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [
    MatDialogContent,
    MatDialogActions,
    MatButton,
    MatDialogTitle
  ],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
})
export class ConfirmDialogComponent {
  /** Injected dialog data from MatDialog config */
  data = inject<DialogData>(MAT_DIALOG_DATA);

  constructor(public dialogRef: MatDialogRef<ConfirmDialogComponent>) {
  }

  /**
   * Close dialog with false (cancel/dismiss).
   */
  onDismiss(): void {
    this.dialogRef.close(false);
  }

  /**
   * Close dialog with true (confirm action).
   */
  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
