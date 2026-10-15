// STUDY NOTES: Checks EmployeeService builds the right HTTP requests: GET with paging/sort/filter params,
// STUDY NOTES: POST to create, PUT to update and DELETE by id. SERVICE test with HttpTestingController (a fake
// STUDY NOTES: HTTP backend), so we test the request the browser WOULD send, without a real server.
import { provideHttpClient } from '@angular/common/http';
// WHY: The testing backend and the controller we use to inspect and answer requests.
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DEFAULT_EMPLOYEE_QUERY, EmployeeRequest } from './employee.model';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let http: HttpTestingController;

  // WHY: Fresh TestBed for every test, so no request from one test leaks into the next.
  beforeEach(() => {
    // WHY: Real HttpClient, but its network layer is replaced by the testing backend.
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EmployeeService);
    http = TestBed.inject(HttpTestingController);
  });

  // WHY: Fails the test if a request was sent but never checked with expectOne (unexpected calls).
  // INTERVIEW: verify() makes HTTP tests strict: the service must send exactly the requests we expect.
  afterEach(() => http.verify());

  // WHY: Proves the list call sends what the backend controller expects, and does not send empty
  // WHY: filters (an empty "country=" would filter for country "" and return nothing).
  it('sends paging and sort parameters and leaves out blank filters', () => {
    // WHY: Act: start from the default query and change a few fields. The search text has extra
    // WHY: spaces on purpose; jobTitle and department stay blank ''.
    service
      .list({ ...DEFAULT_EMPLOYEE_QUERY, search: '  rao ', country: 'India', page: 2, sortBy: 'SALARY', direction: 'DESC' })
      // WHY: subscribe() is needed: without it HttpClient sends nothing.
      .subscribe();

    // WHY: Match on req.url (path only) because the full URL includes the query string.
    const request = http.expectOne(req => req.url === '/api/employees');
    expect(request.request.method).toBe('GET');
    // WHY: Query params are always strings in the URL, so numbers are compared as '2' and '20'.
    expect(request.request.params.get('page')).toBe('2');
    // WHY: 20 comes from DEFAULT_EMPLOYEE_QUERY, proving defaults are sent too.
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('sortBy')).toBe('SALARY');
    expect(request.request.params.get('direction')).toBe('DESC');
    // WHY: The search text is trimmed before sending.
    expect(request.request.params.get('search')).toBe('rao');
    expect(request.request.params.get('country')).toBe('India');
    // WHY: Blank filters are not sent at all (has(...) is false), not even as an empty value.
    expect(request.request.params.has('jobTitle')).toBeFalse();
    expect(request.request.params.has('department')).toBeFalse();
    // WHY: Answer the request with a fake empty page so it completes and verify() passes.
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  // WHY: Proves create() POSTs the form data unchanged and gives the caller the saved employee from the server.
  it('posts a new employee and returns the saved one', () => {
    // WHY: Arrange: a typed request body. Typing it as EmployeeRequest means the compiler checks the fields.
    const body: EmployeeRequest = {
      firstName: 'Asha', lastName: 'Rao', email: 'asha.rao@acme.com', jobTitle: 'Software Engineer',
      department: 'Engineering', country: 'India', salary: 1500000, currency: 'INR', hireDate: '2022-04-01'
    };
    // WHY: A variable the subscribe callback writes to, so we can check what the caller received.
    let savedId = 0;

    // WHY: Act: subscribe and remember the id of the saved employee.
    service.create(body).subscribe(saved => (savedId = saved.id));

    // WHY: Exactly one request to /api/employees.
    const request = http.expectOne('/api/employees');
    expect(request.request.method).toBe('POST');
    // WHY: The body sent is exactly the form data (deep equality).
    expect(request.request.body).toEqual(body);
    // WHY: Simulate the server reply: the same data plus the new id 7. flush() runs the subscribe callback
    // WHY: synchronously, so no fakeAsync is needed here.
    request.flush({ ...body, id: 7, createdAt: '', updatedAt: '' });
    // WHY: Proves the response is passed through to the caller.
    expect(savedId).toBe(7);
  });

  // WHY: Proves the HTTP verb and the id in the URL path for update and delete.
  it('puts updates and deletes by id', () => {
    // WHY: The body content does not matter here, so an empty object cast to the type is enough.
    service.update(7, {} as EmployeeRequest).subscribe();
    service.delete(7).subscribe();

    // WHY: expectOne with { method, url } matches both. If either call used the wrong verb or URL,
    // WHY: expectOne would throw. These requests are not flushed, but verify() only fails on
    // WHY: requests nobody matched, and expectOne already matched them.
    expect(http.expectOne({ method: 'PUT', url: '/api/employees/7' })).toBeTruthy();
    expect(http.expectOne({ method: 'DELETE', url: '/api/employees/7' })).toBeTruthy();
  });
});
