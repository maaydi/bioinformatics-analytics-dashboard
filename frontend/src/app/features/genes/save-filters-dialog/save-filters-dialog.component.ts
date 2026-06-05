import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatInput, MatLabel} from '@angular/material/input';
import {MatError, MatFormField} from '@angular/material/form-field';
import {MatButton} from '@angular/material/button';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';

/** Dialog used to collect the name for a saved filter set. */
@Component({
  selector: 'app-save-filters-dialog',
  standalone: true,
  templateUrl: './save-filters-dialog.component.html',
  styleUrls: ['./save-filters-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormField, MatInput, MatError, MatButton, MatDialogModule, MatLabel],
})
export class SaveFiltersDialogComponent {
  readonly form = new FormGroup<{ name: FormControl<string> }>({
    name: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)]
    }),
  });
  private readonly dialogRef = inject(MatDialogRef<SaveFiltersDialogComponent>);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({name: this.form.controls.name.value});
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}

