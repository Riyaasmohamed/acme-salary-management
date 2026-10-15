// STUDY NOTES: The only code in the app that knows the /api/employees URLs. Components call methods like list(query) instead of building HTTP requests.
// STUDY NOTES: Every method returns an Observable: nothing is sent until someone subscribes, and the caller decides what to do with the result.
// WHY: HttpClient sends HTTP requests; HttpParams builds the ?key=value query string safely (with URL encoding).
import { HttpClient, HttpParams } from '@angular/common/http';
// WHY: Injectable makes this a service; inject() gets HttpClient from Angular's dependency injection.
import { Injectable, inject } from '@angular/core';
// WHY: Observable is the RxJS type for an async result (similar to a Future/CompletableFuture in Java, but it can also be cancelled).
import { Observable } from 'rxjs';

// WHY: The shared '/api' prefix, so the base URL is defined only once.
import { API_BASE_URL } from '../shared/api';
// WHY: Typed request and response shapes, so mistakes in field names are compile errors.
import { Employee, EmployeeQuery, EmployeeRequest, FilterOptions, Page } from './employee.model';

/**
 * Calls the employee endpoints of the backend.
 */
// WHY: providedIn 'root' = one shared instance for the whole app; tests can replace it with a spy.
// INTERVIEW: Components never use HttpClient directly; only the two *.service.ts files know URLs, so API changes stay in one place.
@Injectable({ providedIn: 'root' })
export class EmployeeService {
  // WHY: inject() asks Angular for HttpClient (registered by provideHttpClient() in app.config.ts).
  private readonly http = inject(HttpClient);
  // WHY: '/api/employees'; the dev-server proxy (proxy.conf.json) forwards it to http://localhost:8080/api/employees.
  private readonly url = `${API_BASE_URL}/employees`;

  /** Loads one page of employees; blank search and filters are not sent. */
  list(query: EmployeeQuery): Observable<Page<Employee>> {
    // WHY: HttpParams is immutable: every .set() returns a NEW object, which is why we chain the calls and use "let" below.
    // WHY: Paging and sort are always sent, so the backend never has to guess them.
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sortBy', query.sortBy)
      .set('direction', query.direction);
    // WHY: "as const" makes the array type the exact four key names, so query[key] is known to be a string field.
    for (const key of ['search', 'country', 'jobTitle', 'department'] as const) {
      // WHY: trim() removes spaces, so "  " is treated as empty.
      const value = query[key].trim();
      // WHY: Only non-blank values are added, so the backend sees exactly what the user chose (no "search=" with empty text).
      if (value) {
        // WHY: Re-assign because set() returns a new HttpParams object.
        params = params.set(key, value);
      }
    }
    // WHY: GET /api/employees?page=..&size=..; <Page<Employee>> tells TypeScript the JSON shape of the response.
    return this.http.get<Page<Employee>>(this.url, { params });
  }

  /** Loads the distinct countries, job titles and departments for the filters. */
  filterOptions(): Observable<FilterOptions> {
    // WHY: GET /api/employees/filter-options returns the values that exist in the database, for the dropdowns.
    return this.http.get<FilterOptions>(`${this.url}/filter-options`);
  }

  /** Creates an employee and returns it with its new id. */
  create(request: EmployeeRequest): Observable<Employee> {
    // WHY: POST sends the request body as JSON; the backend answers 201 with the saved employee.
    return this.http.post<Employee>(this.url, request);
  }

  /** Replaces the data of an existing employee. */
  update(id: number, request: EmployeeRequest): Observable<Employee> {
    // WHY: PUT /api/employees/{id} replaces all editable fields of that employee.
    return this.http.put<Employee>(`${this.url}/${id}`, request);
  }

  /** Deletes an employee. */
  delete(id: number): Observable<void> {
    // WHY: DELETE returns 204 No Content, so the result type is void.
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
