// STUDY NOTES: The entry point of the Angular app. angular.json ("browser": "src/main.ts") tells the build to start here.
// STUDY NOTES: It starts ("bootstraps") the root component App with the app-wide settings from app.config.ts.
// WHY: bootstrapApplication starts a standalone Angular app in the browser (no NgModule is needed).
import { bootstrapApplication } from '@angular/platform-browser';
// WHY: appConfig holds the global providers (router, HttpClient, error listeners, change detection).
import { appConfig } from './app/app.config';
// WHY: App is the root component; Angular renders it into the <app-root> tag of index.html.
import { App } from './app/app';

// WHY: Creates the App component with the global providers and shows it on the page.
bootstrapApplication(App, appConfig)
  // WHY: If start-up fails (for example a bad provider), the error is printed in the browser console instead of failing silently.
  .catch((err) => console.error(err));
