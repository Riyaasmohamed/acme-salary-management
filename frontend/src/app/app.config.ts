// STUDY NOTES: App-wide configuration. It lists the global "providers" (services and features) that main.ts passes to bootstrapApplication.
// STUDY NOTES: It plays a similar role to a Spring @Configuration class: it registers things that can be injected everywhere.
// WHY: ApplicationConfig is the type of this object; the provide* functions register framework features.
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
// WHY: provideHttpClient registers HttpClient, so services can inject it to call the backend.
import { provideHttpClient } from '@angular/common/http';
// WHY: provideRouter turns on routing with our route table.
import { provideRouter } from '@angular/router';

// WHY: The list of URL → page rules defined in app.routes.ts.
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    // WHY: Sends uncaught browser errors and unhandled promise rejections to Angular's ErrorHandler, so they are reported (logged) in one place.
    provideBrowserGlobalErrorListeners(),
    // WHY: Zone.js (loaded as a polyfill in angular.json) tells Angular when to refresh the view; eventCoalescing groups several events into one refresh for better speed.
    provideZoneChangeDetection({ eventCoalescing: true }),
    // WHY: Enables navigation between /employees and /insights.
    provideRouter(routes),
    // WHY: Without this, injecting HttpClient in EmployeeService / InsightsService would fail with "No provider for HttpClient".
    provideHttpClient()
  ]
};
