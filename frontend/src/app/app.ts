// STUDY NOTES: The root component (the "shell"). It draws the top toolbar and the <router-outlet> where each page appears.
// WHY: Component is the decorator that turns a TypeScript class into an Angular component.
import { Component } from '@angular/core';
// WHY: RouterOutlet shows the current page; RouterLink makes links navigate without reloading; RouterLinkActive highlights the current link.
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
// WHY: Gives the mat-button style used on the navigation links.
import { MatButtonModule } from '@angular/material/button';
// WHY: Gives the <mat-icon> element.
import { MatIconModule } from '@angular/material/icon';
// WHY: Gives the <mat-toolbar> element (the top bar).
import { MatToolbarModule } from '@angular/material/toolbar';

/**
 * Application shell: top toolbar with navigation and the routed page below it.
 */
@Component({
  // WHY: The HTML tag name; index.html contains <app-root>, so Angular renders this component there.
  selector: 'app-root',
  // WHY: Standalone components list exactly what their template uses; without these, the tags/directives would be unknown.
  // INTERVIEW: Standalone components (no NgModules) are the current recommended Angular style; each component declares its own dependencies.
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  // WHY: The HTML is kept in a separate file to keep the class short and readable.
  templateUrl: './app.html',
  // WHY: Styles in this file apply only to this component (Angular scopes them).
  styleUrl: './app.scss'
})
// WHY: The class is empty because the shell has no logic; all behaviour is in the template (links + router outlet).
export class App {}
