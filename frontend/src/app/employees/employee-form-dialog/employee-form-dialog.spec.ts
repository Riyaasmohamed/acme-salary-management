// STUDY NOTES: Checks the add-employee dialog: invalid forms are not sent, valid input is cleaned and sent in
// STUDY NOTES: the backend's format, and a server "duplicate email" error appears under the email field.
// STUDY NOTES: COMPONENT test with TestBed: the dialog's dependencies (EmployeeService, MatDialogRef, dialog data)
// STUDY NOTES: are replaced by jasmine spies / plain values, and we type into the real inputs like a user.
// WHY: Used to fake the 409 Conflict response from the backend.
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
// WHY: The two things Angular Material gives a dialog: its data (MAT_DIALOG_DATA) and a handle to close it (MatDialogRef).
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
// WHY: of = a successful fake response; throwError = a failed fake response.
import { of, throwError } from 'rxjs';

import { Employee } from '../employee.model';
import { EmployeeService } from '../employee.service';
import { EmployeeFormDialog, EmployeeFormDialogData } from './employee-form-dialog';

describe('EmployeeFormDialog', () => {
  // WHY: Spy service: we decide what create() returns and check what it was called with.
  let service: jasmine.SpyObj<EmployeeService>;
  // WHY: Spy dialog handle: lets us check if (and with what) the dialog was closed.
  let dialogRef: jasmine.SpyObj<MatDialogRef<EmployeeFormDialog, Employee>>;
  let fixture: ComponentFixture<EmployeeFormDialog>;

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  // WHY: Helper that types into a field found by its visible label, like a user would.
  // WHY: Finding by label (not by CSS id) keeps the test readable and close to real use.
  function type(label: string, value: string): void {
    const field = Array.from(element().querySelectorAll('mat-form-field'))
      .find(f => f.querySelector('mat-label')?.textContent?.trim() === label);
    const input = field!.querySelector('input')!;
    input.value = value;
    // WHY: The 'input' event is what Angular reactive forms listen to, so the form control gets the value.
    input.dispatchEvent(new Event('input'));
  }

  // WHY: Clicks the real submit button (this triggers the form's submit and save()), then updates the DOM.
  function submit(): void {
    element().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  }

  // WHY: Fills every required field with valid data. Currency is NOT typed: choosing "India" should fill INR.
  function fillValidForm(): void {
    // WHY: Extra spaces on purpose, to prove the dialog trims values before sending.
    type('First name', ' Asha ');
    type('Last name', 'Rao');
    type('Email', 'asha.rao@acme.com');
    type('Job title', 'Software Engineer');
    type('Department', 'Engineering');
    type('Country', 'India');
    type('Annual salary', '1500000');
    // WHY: Typed as a user would in the date field (month/day/year); the native date adapter turns it into a Date.
    type('Hire date', '4/1/2022');
    fixture.detectChanges();
  }

  beforeEach(() => {
    // WHY: Only create and update are needed by the dialog.
    service = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['create', 'update']);
    // WHY: Only close() is used by the dialog.
    dialogRef = jasmine.createSpyObj<MatDialogRef<EmployeeFormDialog, Employee>>('MatDialogRef', ['close']);
    // WHY: employee: null means "add" mode (not edit). Options feed the autocomplete suggestions.
    const data: EmployeeFormDialogData = {
      employee: null,
      options: { countries: ['India'], jobTitles: [], departments: [] }
    };
    TestBed.configureTestingModule({
      imports: [EmployeeFormDialog],
      // WHY: We test the dialog content directly, without opening a real MatDialog, so we must
      // WHY: provide what the dialog would normally inject.
      providers: [
        { provide: EmployeeService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });
    fixture = TestBed.createComponent(EmployeeFormDialog);
    // WHY: Render the empty form before each test.
    fixture.detectChanges();
  });

  // WHY: Client-side validation: an empty form must not hit the API, and the user must see why.
  it('does not call the API while the form is invalid and shows the errors', () => {
    // WHY: Act: submit the empty form. save() marks all fields as touched so their errors appear.
    submit();

    // WHY: No request was sent.
    expect(service.create).not.toHaveBeenCalled();
    // WHY: mat-error elements only show for touched, invalid fields. More than 5 proves the errors are visible.
    expect(element().querySelectorAll('mat-error').length).toBeGreaterThan(5);
  });

  // WHY: Proves the request matches the backend contract exactly: trimmed text, a number salary,
  // WHY: the currency picked from the country, and the date as "YYYY-MM-DD".
  it('sends trimmed values, the currency of the country and a YYYY-MM-DD hire date', () => {
    // WHY: callFake runs our function instead: it echoes the request back as a saved employee with id 7.
    service.create.and.callFake(request => of({ ...request, id: 7, createdAt: '', updatedAt: '' }));
    fillValidForm();

    submit();

    // WHY: toHaveBeenCalledOnceWith checks it was called exactly once AND with exactly this object.
    // WHY: "Asha" is trimmed, currency is INR (we never typed it), hireDate is '2022-04-01'.
    expect(service.create).toHaveBeenCalledOnceWith({
      firstName: 'Asha', lastName: 'Rao', email: 'asha.rao@acme.com', jobTitle: 'Software Engineer',
      department: 'Engineering', country: 'India', salary: 1500000, currency: 'INR', hireDate: '2022-04-01'
    });
    // WHY: On success the dialog closes and returns the saved employee, so the list can refresh.
    // WHY: objectContaining checks only the id; the other fields do not matter here.
    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ id: 7 }));
  });

  // WHY: Some rules can only be checked by the server (unique email). Its error must show next to
  // WHY: the right field, and the dialog must stay open so the user can fix it.
  it('shows a duplicate email error from the server under the email field', () => {
    // WHY: Arrange: create() fails with the same 409 body the backend sends for a taken email.
    service.create.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: {
        detail: 'An employee with email asha.rao@acme.com already exists',
        errors: { email: 'This email is already used by another employee' }
      }
    })));
    fillValidForm();

    submit();

    // WHY: The form is otherwise valid, so the only visible mat-error is the server message on the email field.
    expect(element().querySelector('mat-error')?.textContent).toContain('This email is already used by another employee');
    // WHY: The dialog stays open; the user's typed data is not lost.
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
