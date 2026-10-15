// STUDY NOTES: TypeScript types that mirror the backend JSON (like Java DTOs, but only for the compiler). No runtime code except the default query.
// STUDY NOTES: Keeping them in one file means the service, the list and the form all agree on the same data shape.
/**
 * An employee as returned by the API.
 */
// WHY: An interface only describes a shape; TypeScript checks it at compile time and it disappears from the built JavaScript.
export interface Employee {
  // WHY: Java Long/BigDecimal both arrive as JSON numbers, so they are "number" here.
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  jobTitle: string;
  department: string;
  country: string;
  // WHY: Salary is only displayed and sent back, never calculated in the UI, so a JSON number is enough here.
  salary: number;
  // WHY: ISO 4217 code such as "INR" or "USD"; each salary is in its own currency.
  currency: string;
  /** ISO date, e.g. "2022-04-01". */
  // WHY: Kept as text because Java LocalDate is sent as "YYYY-MM-DD"; converting to Date could shift the day (see shared/dates.ts).
  hireDate: string;
  // WHY: Audit timestamps set by the backend; the UI never sends them.
  createdAt: string;
  updatedAt: string;
}

/**
 * Body sent to create or update an employee.
 */
// WHY: Omit<...> reuses Employee but removes the fields the server controls, so the client can never send id or timestamps.
export type EmployeeRequest = Omit<Employee, 'id' | 'createdAt' | 'updatedAt'>;

/**
 * One page of results from the API.
 */
// WHY: Generic <T> (like Java generics) so the same page shape could hold any item type; it matches the backend PageResponse record.
export interface Page<T> {
  // WHY: The rows of the current page only (20–100), not all employees.
  content: T[];
  // WHY: Zero-based page number, the same as Spring Data.
  page: number;
  size: number;
  // WHY: Total matching rows; the paginator uses it to show "1 – 20 of 50".
  totalElements: number;
  totalPages: number;
}

// WHY: A union of string literals works like a small enum: only these exact values compile; they match the backend's sort enum.
export type SortField = 'NAME' | 'SALARY' | 'HIRE_DATE';
export type SortDirection = 'ASC' | 'DESC';

/**
 * Everything that decides which employees the list shows.
 */
// INTERVIEW: All list state is one object in one signal, so any change (search, filter, sort, page) triggers exactly one request.
export interface EmployeeQuery {
  // WHY: Free text search; blank means "no search".
  search: string;
  // WHY: Each filter is an empty string when not used; the service leaves blank values out of the URL.
  country: string;
  jobTitle: string;
  department: string;
  page: number;
  size: number;
  sortBy: SortField;
  direction: SortDirection;
}

/**
 * Values that exist in the data, used to fill the filter dropdowns.
 */
// WHY: The dropdowns are filled from real data in the database, so the user can only pick values that exist.
export interface FilterOptions {
  countries: string[];
  jobTitles: string[];
  departments: string[];
}

// WHY: The starting state of the list: first page, 20 rows, sorted by name A–Z, no filters.
export const DEFAULT_EMPLOYEE_QUERY: EmployeeQuery = {
  search: '',
  country: '',
  jobTitle: '',
  department: '',
  page: 0,
  size: 20,
  sortBy: 'NAME',
  direction: 'ASC'
};
