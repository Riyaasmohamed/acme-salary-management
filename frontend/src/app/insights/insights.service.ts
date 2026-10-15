// STUDY NOTES: The only code that knows the /api/insights URLs. Three read-only GET calls, each returning an Observable of typed rows.
// WHY: HttpClient sends the requests; HttpParams builds the ?country=... query string with proper URL encoding (e.g. "United States").
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
// WHY: Observable = an async result the page can subscribe to, combine (forkJoin) or cancel (switchMap).
import { Observable } from 'rxjs';

// WHY: Shared '/api' prefix, forwarded to the backend by the dev-server proxy.
import { API_BASE_URL } from '../shared/api';
import { CountryHeadcount, CountrySalaryStats, JobTitleSalaryStats } from './insights.model';

/**
 * Calls the read-only insight endpoints. All numbers are computed by the database.
 */
// INTERVIEW: Aggregates are calculated in SQL, so the browser receives a few small rows instead of 10,000 employees.
@Injectable({ providedIn: 'root' })
export class InsightsService {
  // WHY: HttpClient is available because app.config.ts calls provideHttpClient().
  private readonly http = inject(HttpClient);
  // WHY: Base URL '/api/insights' for all three endpoints.
  private readonly url = `${API_BASE_URL}/insights`;

  /** Min, max, average salary and headcount per country. */
  salaryByCountry(): Observable<CountrySalaryStats[]> {
    // WHY: GET /api/insights/salary-by-country returns one row per country and currency.
    return this.http.get<CountrySalaryStats[]>(`${this.url}/salary-by-country`);
  }

  /** Salary statistics per job title in one country. */
  salaryByJobTitle(country: string): Observable<JobTitleSalaryStats[]> {
    // WHY: The country is a query parameter; HttpParams encodes spaces and special characters safely.
    const params = new HttpParams().set('country', country);
    return this.http.get<JobTitleSalaryStats[]>(`${this.url}/salary-by-job-title`, { params });
  }

  /** Number of employees per country, largest first. */
  headcountByCountry(): Observable<CountryHeadcount[]> {
    // WHY: The backend already sorts largest first, so the page can take [0] as the largest team.
    return this.http.get<CountryHeadcount[]>(`${this.url}/headcount-by-country`);
  }
}
