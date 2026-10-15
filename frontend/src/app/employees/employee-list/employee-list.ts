// STUDY NOTES: The main page (route /employees): search box, three filter dropdowns, a sortable Material table, a paginator and add / edit / delete actions.
// STUDY NOTES: All paging, filtering and sorting is done by the backend. The page keeps its state in signals and loads data with RxJS (debounce + switchMap).
// WHY: BreakpointObserver (Angular CDK) tells us when the screen width matches a media query, so we can show fewer columns on small screens.
import { BreakpointObserver } from '@angular/cdk/layout';
// WHY: Pipes used in the template: currency (salary), date (hire date), number (total count with thousands separators).
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
// WHY: computed() = value derived from signals; inject() = dependency injection; signal() = a reactive value the template can read.
import { Component, computed, inject, signal } from '@angular/core';
// WHY: Bridges between signals and RxJS: toObservable (signal → stream), toSignal (stream → signal), takeUntilDestroyed (auto-unsubscribe).
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
// WHY: FormControl holds the search box value; ReactiveFormsModule gives the [formControl] directive for the template.
import { FormControl, ReactiveFormsModule } from '@angular/forms';
// WHY: Material buttons (mat-flat-button, mat-icon-button, ...).
import { MatButtonModule } from '@angular/material/button';
// WHY: <mat-card> containers for the filters and the table.
import { MatCardModule } from '@angular/material/card';
// WHY: MatDialog is the service that opens the add/edit and confirm dialogs.
import { MatDialog } from '@angular/material/dialog';
// WHY: <mat-form-field> wrapper with label and outline for inputs and selects.
import { MatFormFieldModule } from '@angular/material/form-field';
// WHY: <mat-icon> for icons.
import { MatIconModule } from '@angular/material/icon';
// WHY: matInput directive for the search input.
import { MatInputModule } from '@angular/material/input';
// WHY: <mat-paginator> and PageEvent (the event type it emits when the page or page size changes).
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
// WHY: <mat-progress-bar> shown while a request is loading.
import { MatProgressBarModule } from '@angular/material/progress-bar';
// WHY: <mat-select> dropdowns for the filters.
import { MatSelectModule } from '@angular/material/select';
// WHY: matSort / mat-sort-header for clickable column headers; Sort is the event type with the active column and direction.
import { MatSortModule, Sort } from '@angular/material/sort';
// WHY: mat-table and its column/row directives.
import { MatTableModule } from '@angular/material/table';
// WHY: matTooltip on the edit and delete icon buttons.
import { MatTooltipModule } from '@angular/material/tooltip';
// WHY: RxJS operators used below; each one is explained where it is used.
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';

// WHY: The reusable "Are you sure?" dialog and the type of data it needs.
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog';
// WHY: Turns an HTTP error into a sentence for the user.
import { errorMessage } from '../../shared/error-message';
// WHY: Shows success / error snackbars.
import { NotificationService } from '../../shared/notification.service';
// WHY: The data types and the default query (page 0, 20 rows, sorted by name).
import { DEFAULT_EMPLOYEE_QUERY, Employee, EmployeeQuery, FilterOptions, Page, SortField } from '../employee.model';
// WHY: The add/edit dialog component and the data it receives.
import { EmployeeFormDialog, EmployeeFormDialogData } from '../employee-form-dialog/employee-form-dialog';
// WHY: The API service; the component never calls HttpClient itself.
import { EmployeeService } from '../employee.service';

// WHY: Only these three query fields can be set by a dropdown; the type stops typos like setFilter('contry', ...).
type FilterKey = 'country' | 'jobTitle' | 'department';

// WHY: Maps the table's column names (used by matSort) to the backend's sort enum values; only these three columns are sortable.
const SORT_FIELDS: Record<string, SortField> = { name: 'NAME', salary: 'SALARY', hireDate: 'HIRE_DATE' };
// WHY: All columns for normal screens; the names must match the matColumnDef names in the template.
const WIDE_COLUMNS = ['name', 'jobTitle', 'department', 'country', 'salary', 'hireDate', 'actions'];
// WHY: On narrow screens only name, salary and actions are shown, so nothing needs horizontal scrolling.
const NARROW_COLUMNS = ['name', 'salary', 'actions'];

/**
 * The employee list: a server-side paginated, searchable, filterable and sortable table.
 * All paging, filtering and sorting happens in the backend; the page only holds the
 * current page of 20-100 rows.
 */
// INTERVIEW: Server-side paging means the browser never loads 10,000 rows; it only holds one page, so it stays fast as data grows.
@Component({
  // WHY: Tag name; the page is shown by the router (lazy-loaded), not by this tag in HTML.
  selector: 'app-employee-list',
  // WHY: Standalone component: it lists every pipe, directive and Material module its template uses.
  imports: [
    CurrencyPipe, DatePipe, DecimalPipe, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule, MatProgressBarModule,
    MatSelectModule, MatSortModule, MatTableModule, MatTooltipModule
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss'
})
export class EmployeeList {
  // WHY: inject() gets the services from Angular; private + readonly because only this class uses them and they never change.
  private readonly employeeService = inject(EmployeeService);
  private readonly notifications = inject(NotificationService);
  // WHY: MatDialog opens dialogs (components shown in an overlay) and gives back their result.
  private readonly dialog = inject(MatDialog);

  // WHY: A standalone reactive FormControl for the search box; nonNullable means reset gives '' instead of null.
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  // WHY: The single source of truth for what the list shows; every user action replaces this object, which triggers one load.
  protected readonly query = signal<EmployeeQuery>(DEFAULT_EMPLOYEE_QUERY);
  // WHY: Values for the three dropdowns; starts empty and is filled from the backend.
  protected readonly filterOptions = signal<FilterOptions>({ countries: [], jobTitles: [], departments: [] });
  // WHY: The last loaded page from the backend; null before the first answer or after an error.
  protected readonly page = signal<Page<Employee> | null>(null);
  // WHY: true while a request is running; the template shows a progress bar and disables the paginator.
  protected readonly loading = signal(true);
  // WHY: The error message to show, or null when everything is fine.
  protected readonly error = signal<string | null>(null);

  // WHY: computed() derives the rows from page(); it updates automatically when page changes; [] avoids null checks in the template.
  protected readonly employees = computed(() => this.page()?.content ?? []);
  // WHY: true when any search or filter is active; used for "matching" vs "in total" and to show "Clear filters".
  protected readonly hasFilters = computed(() => {
    const q = this.query();
    // WHY: !! turns the first non-empty string (or '') into a real true/false.
    return !!(q.search || q.country || q.jobTitle || q.department);
  });

  // WHY: toSignal turns the BreakpointObserver stream into a signal: true when the window is 900px wide or less.
  private readonly isNarrow = toSignal(
    // WHY: observe() emits whenever the media query result changes; map keeps only the true/false "matches" value.
    inject(BreakpointObserver).observe('(max-width: 900px)').pipe(map(state => state.matches)),
    // WHY: A signal needs a value right away, before the stream emits; we assume a wide screen first.
    { initialValue: false }
  );
  // WHY: The column list the table shows; it switches automatically when the screen size crosses 900px.
  protected readonly displayedColumns = computed(() => (this.isNarrow() ? NARROW_COLUMNS : WIDE_COLUMNS));

  // WHY: The constructor runs once when the page is created; it sets up the data streams (they live until the page is destroyed).
  constructor() {
    // Every change of the query loads a page; switchMap drops responses for outdated queries.
    // WHY: toObservable turns the query signal into a stream that emits the new query each time the signal changes (and once at start).
    toObservable(this.query)
      .pipe(
        // WHY: tap runs a side effect without changing the value: show the progress bar and hide an old error before loading.
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        // WHY: switchMap starts the HTTP request for the newest query and cancels the previous one if it is still running, so an old response can never overwrite a newer one.
        // INTERVIEW: switchMap (not mergeMap) is chosen because only the latest query matters; it prevents race conditions when the user clicks quickly.
        switchMap(query =>
          this.employeeService.list(query).pipe(
            // WHY: catchError is inside switchMap so one failed request does not kill the whole stream; later queries still work.
            catchError(error => {
              // WHY: Save a readable message for the error state in the template.
              this.error.set(errorMessage(error));
              // WHY: of(null) replaces the error with a normal value (no page), so the subscribe below still runs.
              return of(null);
            })
          )
        ),
        // WHY: takeUntilDestroyed unsubscribes automatically when the page is destroyed, so there is no memory leak.
        takeUntilDestroyed()
      )
      // WHY: subscribe starts the stream; each result (a page or null) is stored and loading ends.
      .subscribe(page => {
        this.page.set(page);
        this.loading.set(false);
      });

    // Wait until the user stops typing before searching.
    // WHY: valueChanges emits on every keystroke in the search box.
    this.searchControl.valueChanges
      // WHY: debounceTime(300) waits 300 ms of no typing; map trims spaces; distinctUntilChanged skips the search if the text did not really change.
      .pipe(debounceTime(300), map(value => value.trim()), distinctUntilChanged(), takeUntilDestroyed())
      // WHY: update() creates a new query object with the new search text and goes back to page 0 (the old page may not exist for the new results).
      .subscribe(search => this.query.update(q => ({ ...q, search, page: 0 })));

    // WHY: Fill the dropdowns once when the page opens.
    this.loadFilterOptions();
  }

  /** Applies a dropdown filter and goes back to the first page. */
  protected setFilter(key: FilterKey, value: string): void {
    // WHY: { ...q } copies the old query (spread operator); [key]: value overwrites just that one filter; page 0 so the user never lands on a page that no longer exists.
    this.query.update(q => ({ ...q, [key]: value, page: 0 }));
  }

  /** Removes the search text and all filters. */
  protected clearFilters(): void {
    // WHY: emitEvent false clears the box without firing valueChanges, so we do not trigger a second (debounced) query.
    this.searchControl.setValue('', { emitEvent: false });
    // WHY: One update that clears everything = exactly one new request.
    this.query.update(q => ({ ...q, search: '', country: '', jobTitle: '', department: '', page: 0 }));
  }

  /** Maps a click on a column header to the backend's sort parameters. */
  protected onSortChange(sort: Sort): void {
    // WHY: sort.active is the column name (e.g. 'salary'); unknown names fall back to NAME so we never send an invalid sort.
    const sortBy = SORT_FIELDS[sort.active] ?? 'NAME';
    // WHY: Material uses 'asc' / 'desc' in lower case; the backend expects 'ASC' / 'DESC'.
    const direction = sort.direction === 'desc' ? 'DESC' : 'ASC';
    // WHY: A new sort order changes every page, so we go back to page 0.
    this.query.update(q => ({ ...q, sortBy, direction, page: 0 }));
  }

  /** Moves to another page or changes the page size. */
  protected onPage(event: PageEvent): void {
    // WHY: The paginator tells us the new page index and page size; the query change loads that page from the server.
    this.query.update(q => ({ ...q, page: event.pageIndex, size: event.pageSize }));
  }

  /** Opens the add dialog (no employee) or the edit dialog, then refreshes the list. */
  protected openForm(employee: Employee | null): void {
    // WHY: The dialog gets the employee to edit (or null for "add") and the known values for its autocomplete fields.
    const data: EmployeeFormDialogData = { employee, options: this.filterOptions() };
    this.dialog
      // WHY: The three type parameters are: the dialog component, the data type it receives, and the result type it closes with.
      .open<EmployeeFormDialog, EmployeeFormDialogData, Employee>(EmployeeFormDialog, {
        data,
        // WHY: 720px on desktop, but never wider than 95% of the screen on phones.
        width: '720px',
        maxWidth: '95vw'
      })
      // WHY: afterClosed() emits once when the dialog closes, with the saved employee or undefined (cancel).
      .afterClosed()
      .subscribe(saved => {
        // WHY: Cancel or close without saving: nothing to do.
        if (!saved) {
          return;
        }
        const name = `${saved.firstName} ${saved.lastName}`;
        // WHY: If an employee was passed in, this was an edit; otherwise it was an add.
        this.notifications.success(employee ? `${name} was updated` : `${name} was added`);
        // WHY: Load the current page again so the table shows the change.
        this.reload();
        // WHY: A new country, job title or department may now exist, so refresh the dropdowns too.
        this.loadFilterOptions();
      });
  }

  /** Asks for confirmation, deletes the employee and refreshes the list. */
  protected confirmDelete(employee: Employee): void {
    const name = `${employee.firstName} ${employee.lastName}`;
    this.dialog
      // WHY: The confirm dialog closes with true (Delete) or false/undefined (Cancel).
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: 'Delete employee?',
          // WHY: The email is shown too, so the user can be sure it is the right person.
          message: `${name} (${employee.email}) will be removed permanently. This cannot be undone.`,
          confirmLabel: 'Delete'
        },
        width: '440px'
      })
      .afterClosed()
      .pipe(
        // WHY: filter(Boolean) lets only a true result pass, so on cancel nothing else happens.
        filter(Boolean),
        // WHY: After confirmation, switch to the DELETE request (this one emits only once, after confirmation).
        switchMap(() => this.employeeService.delete(employee.id))
      )
      // WHY: The object form of subscribe handles success (next) and failure (error) separately.
      .subscribe({
        next: () => {
          this.notifications.success(`${name} was deleted`);
          // When the only row of a page is deleted, show the previous page instead of an empty one.
          const pageBecomesEmpty = this.employees().length === 1 && this.query().page > 0;
          // WHY: Always a new query object, so the list reloads (on the same page or the previous one).
          this.query.update(q => ({ ...q, page: pageBecomesEmpty ? q.page - 1 : q.page }));
          // WHY: The deleted employee may have been the last one in a country, so refresh the dropdowns.
          this.loadFilterOptions();
        },
        // WHY: Show a snackbar with the reason; the list stays as it was.
        error: error => this.notifications.error(`Could not delete ${name}: ${errorMessage(error)}`)
      });
  }

  /** Loads the current query again, e.g. after an error. */
  protected reload(): void {
    // WHY: { ...q } is a new object with the same values; the signal sees a new reference, so toObservable emits and the page reloads.
    this.query.update(q => ({ ...q }));
  }

  // WHY: private because only this class calls it (constructor, after save, after delete).
  private loadFilterOptions(): void {
    // WHY: A single HTTP call completes by itself, so no unsubscribe is needed here.
    this.employeeService.filterOptions().subscribe({
      // WHY: Store the values; the dropdowns update automatically because they read the signal.
      next: options => this.filterOptions.set(options),
      // WHY: The list still works without dropdown values, so a snackbar is enough.
      error: error => this.notifications.error(`Could not load filters: ${errorMessage(error)}`)
    });
  }
}
