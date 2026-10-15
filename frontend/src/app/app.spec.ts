// STUDY NOTES: Checks the App shell component (the top toolbar): it shows the product name and has nav links
// STUDY NOTES: to the two pages. COMPONENT test with Angular TestBed: the real template is rendered into a
// STUDY NOTES: test DOM, so we check what the user would actually see, not just class fields.
// WHY: TestBed is Angular's test helper. It builds a small Angular app (injector + components) for the test.
import { TestBed } from '@angular/core/testing';
// WHY: The template uses routerLink, which needs the Router. provideRouter gives us one.
import { provideRouter } from '@angular/router';
import { App } from './app';

// WHY: describe groups the tests for one unit ("App") in the Jasmine report.
describe('App', () => {
  // WHY: beforeEach runs before every "it", so each test starts with a fresh TestBed setup.
  // WHY: async/await because compileComponents() returns a Promise.
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // WHY: App is a standalone component, so it goes in "imports" (not "declarations").
      imports: [App],
      // WHY: An EMPTY route list is enough: we only check the links' href, we do not navigate.
      // WHY: Without a Router, routerLink would fail with "No provider for Router".
      providers: [provideRouter([])]
    // WHY: compileComponents loads the external template (app.html) and styles before the test runs.
    }).compileComponents();
  });

  // WHY: "it" is one test. The name reads like a sentence describing the expected behaviour.
  it('shows the product name and links to both pages', () => {
    // WHY: Arrange: create the component. The fixture is a wrapper to reach the component and its DOM.
    const fixture = TestBed.createComponent(App);
    // WHY: Run change detection once so Angular renders the template (and routerLink sets the href).
    // INTERVIEW: In tests Angular does not render automatically; you call fixture.detectChanges() to update the DOM.
    fixture.detectChanges();
    // WHY: nativeElement is the real DOM element of the component, so we can query it like in a browser.
    const element = fixture.nativeElement as HTMLElement;

    // WHY: Assert: the brand text is visible. ?. avoids a crash if .brand is missing (the test then fails clearly).
    expect(element.querySelector('.brand')?.textContent).toContain('ACME Salary Management');
    // WHY: Collect the href of every link inside <nav>. routerLink turns "/employees" into a real href.
    const links = Array.from(element.querySelectorAll('nav a')).map(a => a.getAttribute('href'));
    // WHY: toEqual compares array contents and order: exactly two links, Employees first, then Insights.
    expect(links).toEqual(['/employees', '/insights']);
  });
});
