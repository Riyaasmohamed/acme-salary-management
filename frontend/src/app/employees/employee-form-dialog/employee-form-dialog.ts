// STUDY NOTES: One Material dialog for both "add employee" (no employee passed in) and "edit employee" (employee passed in, fields pre-filled).
// STUDY NOTES: It uses a typed reactive form with the same rules as the backend, sends create or update, and maps backend errors back onto the fields.
// WHY: Signal is the type of a read-only signal; computed/signal create reactive values; inject gets dependencies.
import { Component, Signal, computed, inject, signal } from '@angular/core';
// WHY: takeUntilDestroyed auto-unsubscribes when the dialog closes; toSignal turns a stream (valueChanges) into a signal.
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
// WHY: Reactive forms building blocks: FormGroup (the whole form), FormControl (one field), Validators (built-in rules), ValidationErrors/AbstractControl (for our own rule).
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
// WHY: <mat-autocomplete> suggests existing countries, job titles and departments while typing.
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
// WHY: The datepicker needs a "date adapter" that knows how to read and create dates; the native one uses the JavaScript Date.
import { provideNativeDateAdapter } from '@angular/material/core';
// WHY: <mat-datepicker> calendar popup for the hire date.
import { MatDatepickerModule } from '@angular/material/datepicker';
// WHY: MAT_DIALOG_DATA = the data passed in by the caller; MatDialogRef = a handle to close this dialog with a result.
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
// WHY: <mat-spinner> shown inside the save button while saving.
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
// WHY: <mat-select> for the currency dropdown.
import { MatSelectModule } from '@angular/material/select';

// WHY: Convert the hire date between Date (the datepicker) and "YYYY-MM-DD" (the API) without time-zone shifts.
import { parseIsoDate, toIsoDate } from '../../shared/dates';
// WHY: errorMessage = one sentence for the banner; fieldErrors = backend messages per field.
import { errorMessage, fieldErrors } from '../../shared/error-message';
import { Employee, EmployeeRequest, FilterOptions } from '../employee.model';
import { EmployeeService } from '../employee.service';

/**
 * Input of the dialog: the employee to edit (or null to add one) and the known values for
 * the country, job title and department suggestions.
 */
export interface EmployeeFormDialogData {
  // WHY: null means "add mode"; an employee means "edit mode".
  employee: Employee | null;
  // WHY: Known values from the list page, used for the autocomplete suggestions.
  options: FilterOptions;
}

// WHY: The currencies the user can pick; exported so tests can use the same list.
export const CURRENCIES = ['AUD', 'CAD', 'CHF', 'CNY', 'EUR', 'GBP', 'INR', 'JPY', 'SGD', 'USD'];

// WHY: The usual currency of each known country, used to pre-fill the currency field (a convenience, not a rule).
const CURRENCY_BY_COUNTRY: Record<string, string> = {
  Australia: 'AUD',
  Canada: 'CAD',
  Germany: 'EUR',
  India: 'INR',
  Japan: 'JPY',
  Singapore: 'SGD',
  'United Kingdom': 'GBP',
  'United States': 'USD'
};

// WHY: Maps each validator's error key to a friendly message, so all fields show the same wording.
const ERROR_TEXT: Record<string, string> = {
  required: 'This field is required',
  email: 'Enter a valid email address',
  maxlength: 'This value is too long',
  min: 'Salary must be greater than 0',
  max: 'Salary is too large',
  // WHY: Our own error key, produced by the notInFuture validator below.
  future: 'Hire date cannot be in the future',
  // WHY: Set by the datepicker when the typed text is not a valid date.
  matDatepickerParse: 'Enter a valid date'
};

/** Rejects dates after today. */
// WHY: A custom validator is just a function: it returns null when valid, or an error object when invalid.
function notInFuture(control: AbstractControl<Date | null>): ValidationErrors | null {
  // WHY: Compare with the end of today, so any time today is still allowed.
  const endOfToday = new Date();
  endOfToday.setHours(23, 59, 59, 999);
  // WHY: An empty value is not this validator's job (Validators.required handles it).
  return control.value && control.value > endOfToday ? { future: true } : null;
}

/**
 * Dialog to add a new employee or edit an existing one. Closes with the saved employee, or
 * with nothing when cancelled. The client-side rules mirror the backend so most mistakes are
 * caught before a request is sent; backend errors are still shown next to the right field.
 */
// INTERVIEW: Client validation is for fast feedback only; the backend is the source of truth (e.g. duplicate email), and its errors are shown on the same field.
@Component({
  // WHY: Tag name; MatDialog creates this component directly, so the tag is not used in HTML.
  selector: 'app-employee-form-dialog',
  // WHY: Everything the template uses: reactive form directives and the Material components.
  imports: [
    ReactiveFormsModule, MatAutocompleteModule, MatButtonModule, MatDatepickerModule, MatDialogModule,
    MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule
  ],
  // WHY: Provides the date adapter only for this component, because only this dialog has a datepicker.
  providers: [provideNativeDateAdapter()],
  templateUrl: './employee-form-dialog.html',
  styleUrl: './employee-form-dialog.scss'
})
export class EmployeeFormDialog {
  // WHY: The data passed by EmployeeList when it opened the dialog.
  private readonly data = inject<EmployeeFormDialogData>(MAT_DIALOG_DATA);
  // WHY: Typed handle to close the dialog; the second type (Employee) is the result the caller receives in afterClosed().
  private readonly dialogRef = inject<MatDialogRef<EmployeeFormDialog, Employee>>(MatDialogRef);
  private readonly employeeService = inject(EmployeeService);

  // WHY: Decided once: changes the title and button text between "Add" and "Edit".
  protected readonly isEdit = this.data.employee !== null;
  // WHY: Exposes the constant to the template (templates can only read class members).
  protected readonly currencies = CURRENCIES;
  // WHY: Used as [max] for the datepicker, so future days cannot be picked in the calendar.
  protected readonly today = new Date();
  // WHY: true while the request runs; disables the buttons and shows a spinner to prevent double submits.
  protected readonly saving = signal(false);
  // WHY: A general error (not tied to one field) shown in a banner at the top of the form.
  protected readonly formError = signal<string | null>(null);

  // WHY: A typed FormGroup: TypeScript knows each field's type, so form.getRawValue() is fully typed.
  // INTERVIEW: Reactive forms keep the rules in TypeScript (easy to unit test) instead of in the HTML.
  protected readonly form = new FormGroup({
    // WHY: nonNullable means reset() returns '' instead of null; required + maxLength(100) match the backend and the database column size.
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    // WHY: Validators.email checks the email format; 254 characters is the practical maximum length of an email address.
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)]
    }),
    jobTitle: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    department: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    country: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    // WHY: number | null because the field starts empty; min(0.01) = "greater than 0"; max keeps the value inside DECIMAL(14,2).
    salary: new FormControl<number | null>(null, [Validators.required, Validators.min(0.01), Validators.max(999_999_999_999)]),
    // WHY: Required; the user picks it or it is pre-filled from the country.
    currency: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    // WHY: A Date object for the datepicker; our notInFuture rule matches the backend's "not in the future" rule.
    hireDate: new FormControl<Date | null>(null, [Validators.required, notInFuture])
  });

  // WHY: Each suggestion list is a signal that filters the known values by what the user has typed in that field.
  protected readonly countrySuggestions = this.suggestions(this.form.controls.country, this.data.options.countries);
  protected readonly jobTitleSuggestions = this.suggestions(this.form.controls.jobTitle, this.data.options.jobTitles);
  protected readonly departmentSuggestions = this.suggestions(
    this.form.controls.department,
    this.data.options.departments
  );

  constructor() {
    const employee = this.data.employee;
    // WHY: In edit mode, fill every field with the employee's current values.
    if (employee) {
      // WHY: setValue (not patchValue) must receive every field, so the compiler catches a forgotten field.
      this.form.setValue({
        firstName: employee.firstName,
        lastName: employee.lastName,
        email: employee.email,
        jobTitle: employee.jobTitle,
        department: employee.department,
        country: employee.country,
        salary: employee.salary,
        currency: employee.currency,
        // WHY: The API sends "YYYY-MM-DD"; the datepicker needs a local Date.
        hireDate: parseIsoDate(employee.hireDate)
      });
    }
    // Suggest the usual currency for a known country, unless the user picked one themselves.
    // WHY: React to every change of the country field; takeUntilDestroyed stops listening when the dialog closes.
    this.form.controls.country.valueChanges.pipe(takeUntilDestroyed()).subscribe(country => {
      const currency = CURRENCY_BY_COUNTRY[country.trim()];
      // WHY: "dirty" means the user changed the currency by hand; then we never overwrite their choice.
      if (currency && !this.form.controls.currency.dirty) {
        this.form.controls.currency.setValue(currency);
      }
    });
  }

  /** Validates, sends the create or update request and closes the dialog on success. */
  protected save(): void {
    // WHY: Do not call the backend with invalid data.
    if (this.form.invalid) {
      // WHY: Marks every field as touched so all error messages become visible at once.
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    // WHY: Clear an old banner error before a new try.
    this.formError.set(null);
    const request = this.toRequest();
    const employee = this.data.employee;
    // WHY: Same form, two actions: PUT when editing, POST when adding. The $ suffix is a naming habit for Observables.
    const save$ = employee
      ? this.employeeService.update(employee.id, request)
      : this.employeeService.create(request);
    // WHY: Nothing is sent until subscribe() is called.
    save$.subscribe({
      // WHY: Close the dialog and give the saved employee to the list page (it shows a snackbar and reloads).
      next: saved => this.dialogRef.close(saved),
      error: error => {
        // WHY: Enable the buttons again so the user can fix the problem and retry.
        this.saving.set(false);
        this.showServerErrors(error);
      }
    });
  }

  /** Returns the message to show under a field, or an empty string if it is valid. */
  // WHY: "keyof typeof this.form.controls" allows only real field names, so errorText('emial') would not compile.
  protected errorText(field: keyof typeof this.form.controls): string {
    const errors = this.form.controls[field].errors;
    if (!errors) {
      return '';
    }
    // WHY: A message from the backend (e.g. "This email is already used") has priority.
    const server = errors['server'] as string | undefined;
    if (server) {
      return server;
    }
    // WHY: Show only the first error of the field, so the user gets one clear message.
    const firstKey = Object.keys(errors)[0];
    return ERROR_TEXT[firstKey] ?? 'This value is not valid';
  }

  // WHY: Converts the form values into the exact JSON body the backend expects.
  private toRequest(): EmployeeRequest {
    // WHY: getRawValue() returns all values with their types (including disabled fields, if any).
    const value = this.form.getRawValue();
    return {
      // WHY: trim() removes accidental spaces; the backend also normalises input, but sending clean data is better.
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim(),
      jobTitle: value.jobTitle.trim(),
      department: value.department.trim(),
      country: value.country.trim(),
      // WHY: ?? is only for the type checker; the form is valid here, so salary is never null.
      salary: value.salary ?? 0,
      currency: value.currency,
      // WHY: Sent as "YYYY-MM-DD" using the local calendar day (no UTC shift).
      hireDate: toIsoDate(value.hireDate ?? new Date())
    };
  }

  /** Puts backend field errors on the matching inputs; anything else is shown above the form. */
  private showServerErrors(error: unknown): void {
    let shownOnField = false;
    // WHY: Object.entries turns the { field: message } map into [field, message] pairs we can loop over.
    for (const [field, message] of Object.entries(fieldErrors(error))) {
      // WHY: The backend uses the same field names as the form (e.g. "email"), so we can find the control by name.
      const control = this.form.get(field);
      if (control) {
        // WHY: A custom "server" error makes the field invalid and errorText() shows the backend's message under it.
        control.setErrors({ server: message });
        // WHY: mat-error is only shown for touched fields.
        control.markAsTouched();
        shownOnField = true;
      }
    }
    // WHY: If no field could show the error (e.g. server down, 500), show it in the banner instead, so it is never lost.
    if (!shownOnField) {
      this.formError.set(errorMessage(error));
    }
  }

  /** Values from `options` that contain what the user has typed so far. */
  private suggestions(control: FormControl<string>, options: string[]): Signal<string[]> {
    // WHY: toSignal turns the field's valueChanges stream into a signal; initialValue covers the pre-filled value in edit mode.
    const typed = toSignal(control.valueChanges, { initialValue: control.value });
    // WHY: computed() re-filters only when the typed text changes; the match is case-insensitive and anywhere in the text.
    return computed(() => {
      const text = typed().trim().toLowerCase();
      return options.filter(option => option.toLowerCase().includes(text));
    });
  }
}
