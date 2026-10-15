// STUDY NOTES: Checks the EmployeeList page: it shows the first page and the total, debounces the search box,
// STUDY NOTES: shows an empty state, and shows an error with a working "Try again" button. COMPONENT test with
// STUDY NOTES: TestBed and a jasmine spy instead of the real EmployeeService, so we control the data and need
// STUDY NOTES: no HTTP. We click and type in the real DOM, like a user, and check what appears on screen.
// WHY: Used to fake a "server unreachable" error (status 0).
import { HttpErrorResponse } from '@angular/common/http';
// WHY: fakeAsync + tick let us control time, so the 300 ms debounce can be tested instantly.
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
// WHY: of(...) makes an observable that returns a value at once; throwError(...) makes one that fails.
import { of, throwError } from 'rxjs';

import { Employee, EmployeeQuery, Page } from '../employee.model';
import { EmployeeService } from '../employee.service';
import { EmployeeList } from './employee-list';

// WHY: One fixed, typed sample employee used by all tests (no random data, so results are stable).
const ASHA: Employee = {
  id: 1, firstName: 'Asha', lastName: 'Rao', email: 'asha.rao@acme.com', jobTitle: 'Software Engineer',
  department: 'Engineering', country: 'India', salary: 1500000, currency: 'INR', hireDate: '2022-04-01',
  createdAt: '', updatedAt: ''
};

// WHY: Helper to build a fake server page. totalElements can be bigger than the rows shown,
// WHY: like a real paged response (20 rows on screen, 10,000 in total).
function pageOf(content: Employee[], totalElements = content.length): Page<Employee> {
  return { content, page: 0, size: 20, totalElements, totalPages: 1 };
}

describe('EmployeeList', () => {
  // WHY: SpyObj = an object whose methods are jasmine spies. We set their return values and
  // WHY: can check how they were called.
  let service: jasmine.SpyObj<EmployeeService>;
  let fixture: ComponentFixture<EmployeeList>;

  // WHY: Short helper to get the rendered DOM.
  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  // WHY: Creates the component AFTER each test has set what service.list returns. The constructor
  // WHY: starts loading at once, so the stub must be ready first.
  function create(): void {
    fixture = TestBed.createComponent(EmployeeList);
    // WHY: First change detection: renders the template and runs the initial load.
    fixture.detectChanges();
  }

  beforeEach(() => {
    // WHY: Fake EmployeeService with only the 3 methods the list page calls.
    // INTERVIEW: Mocking the service (not HttpClient) keeps component tests about the UI only; the HTTP details
    // INTERVIEW: are already tested in employee.service.spec.ts.
    // WHY: The constructor also loads the filter dropdown values, so this must return something in every test.
    service = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['list', 'filterOptions', 'delete']);
    service.filterOptions.and.returnValue(of({ countries: ['India'], jobTitles: [], departments: [] }));
    TestBed.configureTestingModule({
      // WHY: EmployeeList is standalone; importing it brings its Material modules too.
      imports: [EmployeeList],
      // WHY: Replace the real service with our spy. Any inject(EmployeeService) now gets the spy.
      providers: [{ provide: EmployeeService, useValue: service }]
    });
  });

  // WHY: Happy path: the first page and the total count are shown.
  it('shows the employees of the first page and the total', () => {
    // WHY: Arrange: 1 row on screen but 10,000 in total.
    service.list.and.returnValue(of(pageOf([ASHA], 10000)));

    // WHY: Act.
    create();

    // WHY: The total uses the number pipe, so it shows "10,000" with a comma, and says "in total" (no filters active).
    expect(element().querySelector('.subtitle')?.textContent).toContain('10,000 in total');
    // WHY: mat-mdc-row is the CSS class Angular Material puts on each data row of the table.
    expect(element().querySelectorAll('tr.mat-mdc-row').length).toBe(1);
    // WHY: The name column shows first and last name together.
    expect(element().querySelector('tr.mat-mdc-row')?.textContent).toContain('Asha Rao');
  });

  // WHY: Proves the search waits 300 ms after the last key press (debounce), so we do not call the
  // WHY: backend on every key. It also proves a new search goes back to page 0.
  // INTERVIEW: fakeAsync + tick() fake the clock: tick(300) moves time forward 300 ms instantly, so a
  // INTERVIEW: debounce test is fast and never flaky.
  it('searches only after the user stops typing, starting from the first page', fakeAsync(() => {
    service.list.and.returnValue(of(pageOf([ASHA])));
    create();
    // WHY: Find the real search input. The "!" tells TypeScript we are sure it exists.
    const input = element().querySelector<HTMLInputElement>('.search input')!;

    // WHY: Simulate typing "ra". Setting value alone is not enough; the 'input' event is what
    // WHY: Angular forms listen to.
    input.value = 'ra';
    input.dispatchEvent(new Event('input'));
    // WHY: Only 100 ms pass: less than the 300 ms debounce, so no search yet.
    tick(100);
    // WHY: The user keeps typing. This restarts the debounce timer.
    input.value = 'rao';
    input.dispatchEvent(new Event('input'));
    // WHY: Now 300 ms of silence: the debounce fires once with "rao".
    tick(300);
    // WHY: Let Angular process the signal change, which triggers the new list() call.
    fixture.detectChanges();

    // WHY: calls.allArgs() gives the arguments of every call to the spy. We take the first argument (the query).
    const queries = service.list.calls.allArgs().map(([query]) => query as EmployeeQuery);
    // WHY: Exactly 2 calls: the first load, and ONE search. "ra" alone never triggered a request.
    expect(queries.length).toBe(2);
    expect(queries[1].search).toBe('rao');
    // WHY: A new search must start at the first page; staying on page 5 could show an empty page.
    expect(queries[1].page).toBe(0);
  }));

  // WHY: Empty state: when nothing matches, the user sees a clear message instead of an empty table.
  it('shows an empty state when nothing matches', () => {
    service.list.and.returnValue(of(pageOf([])));

    create();

    expect(element().querySelector('.state h2')?.textContent).toContain('No employees found');
  });

  // WHY: Proves error handling: a friendly message, and the user can retry without reloading the browser.
  it('shows the error and loads again on "Try again"', () => {
    // WHY: Arrange: the first load fails like when the backend is down (status 0).
    service.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    create();

    // WHY: role="alert" is the error box; it shows the message from errorMessage().
    expect(element().querySelector('[role="alert"]')?.textContent).toContain('Cannot reach the server');

    // WHY: The server is back: the next list() call succeeds.
    service.list.and.returnValue(of(pageOf([ASHA])));
    // WHY: Act: click the "Try again" button inside the alert, like the user would.
    element().querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    // WHY: Update the DOM after the reload.
    fixture.detectChanges();

    // WHY: The error is gone and the table shows the row. This also proves the stream keeps working
    // WHY: after an error (catchError inside switchMap, so one failure does not kill later loads).
    expect(element().querySelector('[role="alert"]')).toBeNull();
    expect(element().querySelectorAll('tr.mat-mdc-row').length).toBe(1);
  });
});
