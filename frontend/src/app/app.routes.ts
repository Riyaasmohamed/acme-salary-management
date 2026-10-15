// STUDY NOTES: The route table: which URL shows which page. It is given to provideRouter(routes) in app.config.ts.
// WHY: Routes is the TypeScript type for a list of route definitions, so mistakes are caught at compile time.
import { Routes } from '@angular/router';

export const routes: Routes = [
  // WHY: The empty URL "/" redirects to /employees, so the app always opens on the list page.
  // WHY: pathMatch 'full' means only an exactly empty path matches, not every URL (every URL starts with '').
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    // WHY: The URL /employees shows the employee list page.
    path: 'employees',
    // WHY: Angular sets the browser tab title automatically when this route is active.
    title: 'Employees · ACME Salary Management',
    // WHY: loadComponent with a dynamic import() is lazy loading: this page's code is downloaded only when the user opens it.
    // INTERVIEW: Lazy loading keeps the first download small; each page's code is split into its own file by the build.
    loadComponent: () => import('./employees/employee-list/employee-list').then(m => m.EmployeeList)
  },
  {
    // WHY: The URL /insights shows the salary insights dashboard.
    path: 'insights',
    title: 'Insights · ACME Salary Management',
    // WHY: Also lazy-loaded, so users who never open insights never download its code.
    loadComponent: () => import('./insights/insights-page/insights-page').then(m => m.InsightsPage)
  },
  // WHY: '**' matches any unknown URL; it redirects to the list instead of showing a blank page. It must be last because routes are checked in order.
  { path: '**', redirectTo: 'employees' }
];
