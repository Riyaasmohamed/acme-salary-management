// STUDY NOTES: Checks InsightsService calls the three backend insight URLs, with the country as a query
// STUDY NOTES: parameter. SERVICE test with HttpTestingController: Angular's fake HTTP backend catches the
// STUDY NOTES: requests, so we can check URL and params without a running server.
// WHY: The real HttpClient provider; the service injects HttpClient.
import { provideHttpClient } from '@angular/common/http';
// WHY: provideHttpClientTesting swaps the real network for a fake one; HttpTestingController lets us inspect it.
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { InsightsService } from './insights.service';

describe('InsightsService', () => {
  // WHY: Declared here, set in beforeEach, so every test can use them.
  let service: InsightsService;
  let http: HttpTestingController;

  beforeEach(() => {
    // WHY: Order matters: provideHttpClient() first, then provideHttpClientTesting() overrides the backend.
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    // WHY: Get the real service (providedIn: 'root') and the fake HTTP controller from Angular's injector.
    service = TestBed.inject(InsightsService);
    http = TestBed.inject(HttpTestingController);
  });

  // WHY: After each test, fail if any request was made that the test did not expect and handle.
  // INTERVIEW: http.verify() catches extra or forgotten HTTP calls, for example a duplicate request.
  afterEach(() => http.verify());

  // WHY: Proves the URLs match the backend controller exactly. A typo would break the Insights page.
  it('calls the three insight endpoints', () => {
    // WHY: Act: HttpClient observables are "cold": no request is sent until someone subscribes.
    service.salaryByCountry().subscribe();
    service.headcountByCountry().subscribe();
    service.salaryByJobTitle('United Kingdom').subscribe();

    // WHY: expectOne fails if there is not exactly one matching request. flush([]) sends a fake
    // WHY: empty response back, which completes the request so verify() is happy.
    http.expectOne('/api/insights/salary-by-country').flush([]);
    http.expectOne('/api/insights/headcount-by-country').flush([]);
    // WHY: Here we match by a function on req.url (the URL WITHOUT query params), because the full
    // WHY: URL also contains "?country=...".
    const byJob = http.expectOne(req => req.url === '/api/insights/salary-by-job-title');
    // WHY: Proves the country is sent as a query parameter. HttpParams also encodes the space safely.
    expect(byJob.request.params.get('country')).toBe('United Kingdom');
    byJob.flush([]);
  });
});
