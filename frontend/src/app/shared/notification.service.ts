// STUDY NOTES: A small service that shows Material snackbars (short messages at the bottom of the screen) after save, delete or errors.
// STUDY NOTES: It keeps all feedback messages consistent (same duration, same error colour) across pages.
// WHY: Injectable marks the class as a service Angular can create; inject() asks Angular for a dependency.
import { Injectable, inject } from '@angular/core';
// WHY: MatSnackBar is the Material service that opens snackbars.
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Shows short success and error messages at the bottom of the screen.
 */
// WHY: providedIn 'root' creates one shared instance for the whole app (like a Spring singleton bean); no need to list it in providers.
@Injectable({ providedIn: 'root' })
export class NotificationService {
  // WHY: inject() is Angular's modern dependency injection (instead of a constructor parameter); readonly because it never changes.
  private readonly snackBar = inject(MatSnackBar);

  /** Shows a confirmation that disappears after a few seconds. */
  success(message: string): void {
    // WHY: 'Close' adds a button to dismiss early; duration 3000 ms closes it automatically.
    this.snackBar.open(message, 'Close', { duration: 3000 });
  }

  /** Shows an error in the error colour; stays longer so it can be read. */
  error(message: string): void {
    // WHY: 6000 ms because error text is usually longer; panelClass 'snackbar-error' uses the error colours defined in styles.scss.
    this.snackBar.open(message, 'Close', { duration: 6000, panelClass: 'snackbar-error' });
  }
}
