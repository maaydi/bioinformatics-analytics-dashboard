import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ConfirmDialogComponent, DialogData} from './confirm-dialog.component';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {By} from '@angular/platform-browser';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;

  const mockDialogRef = {
    close: vi.fn(),
  };

  const mockDialogData: DialogData = {
    title: 'Delete User',
    message: 'Are you sure you want to delete this user? This cannot be undone.',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      providers: [
        {provide: MatDialogRef, useValue: mockDialogRef},
        {provide: MAT_DIALOG_DATA, useValue: mockDialogData},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Initialization', () => {
    it('should create the component safely', () => {
      expect(component).toBeTruthy();
    });

    it('should properly inject the dialog data', () => {
      expect(component.data).toEqual(mockDialogData);
    });
  });

  describe('Template Bindings', () => {
    it('should render the correct title in the DOM', () => {
      const titleElement = fixture.debugElement.query(By.css('h2[mat-dialog-title]')).nativeElement;
      expect(titleElement.textContent.trim()).toBe(mockDialogData.title);
    });

    it('should render the correct message in the dialog content', () => {
      const messageElement = fixture.debugElement.query(By.css('mat-dialog-content p')).nativeElement;
      expect(messageElement.textContent.trim()).toBe(mockDialogData.message);
    });

    it('should render the correct labels for action buttons', () => {
      const buttons = fixture.debugElement.queryAll(By.css('button'));

      const cancelBtn = buttons[0].nativeElement;
      const confirmBtn = buttons[1].nativeElement;

      expect(cancelBtn.textContent.trim()).toBe(mockDialogData.cancelLabel);
      expect(confirmBtn.textContent.trim()).toBe(mockDialogData.confirmLabel);
    });
  });

  describe('User Interactions', () => {
    it('should close the dialog with `false` when the Dismiss/Cancel button is clicked', () => {
      const cancelBtn = fixture.debugElement.queryAll(By.css('button'))[0];

      cancelBtn.triggerEventHandler('click', null);

      expect(mockDialogRef.close).toHaveBeenCalledTimes(1);
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should close the dialog with `true` when the Confirm button is clicked', () => {
      const confirmBtn = fixture.debugElement.queryAll(By.css('button'))[1];

      confirmBtn.triggerEventHandler('click', null);

      expect(mockDialogRef.close).toHaveBeenCalledTimes(1);
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });

    it('should safely call onDismiss() method directly', () => {
      component.onDismiss();
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should safely call onConfirm() method directly', () => {
      component.onConfirm();
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });
  });
});
