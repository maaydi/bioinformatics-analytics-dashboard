import {Component, inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';

export interface DialogData {
  title: string,
  message: string
  confirmLabel: string
  cancelLabel: string
}

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
  data = inject<DialogData>(MAT_DIALOG_DATA);

  constructor(public dialogRef: MatDialogRef<ConfirmDialogComponent>) {
  }

  onDismiss(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
