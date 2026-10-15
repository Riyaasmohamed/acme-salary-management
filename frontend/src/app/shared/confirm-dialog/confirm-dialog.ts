// STUDY NOTES: A reusable "Are you sure?" dialog. Opened with MatDialog (e.g. before deleting an employee); it closes with true (confirm) or false (cancel).
// WHY: Component declares the class as a component; inject() gets dependencies from Angular.
import { Component, inject } from '@angular/core';
// WHY: MAT_DIALOG_DATA is the injection token that holds the data passed to dialog.open(..., { data }); MatDialogModule gives the mat-dialog-* directives.
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
// WHY: Gives the mat-button and mat-flat-button styles used in the template.
import { MatButtonModule } from '@angular/material/button';

/**
 * Texts shown in the confirmation dialog.
 */
// WHY: A typed contract for the caller, so the dialog always receives a title, a message and a button label.
export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel: string;
}

/**
 * Asks the user to confirm a destructive action. Closes with `true` when confirmed.
 * Focus starts on "Cancel" so pressing Enter by accident does not delete anything.
 */
@Component({
  // WHY: Tag name; not used in HTML here because MatDialog creates this component directly.
  selector: 'app-confirm-dialog',
  // WHY: The template uses mat-dialog-title/content/actions and Material buttons.
  imports: [MatDialogModule, MatButtonModule],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss'
})
export class ConfirmDialog {
  // WHY: Reads the texts the caller passed in; protected so only the template (not other classes) can use it.
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
