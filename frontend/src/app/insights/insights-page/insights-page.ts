// STUDY NOTES: The insights dashboard (route /insights): summary cards, salary range by country, headcount chart, and average salary by job title for one chosen country.
// STUDY NOTES: The overview is loaded with forkJoin (two requests in parallel); the job-title section reacts to the selected-country signal with switchMap.
// WHY: DecimalPipe = the "number" pipe in the template; formatCurrency/formatNumber are the same formatting as functions, used in TypeScript for chart labels.
import { DecimalPipe, formatCurrency, formatNumber } from '@angular/common';
// WHY: LOCALE_ID is the app's locale (default en-US); the format functions need it, while pipes get it automatically.
import { Component, LOCALE_ID, computed, inject, signal } from '@angular/core';
// WHY: toObservable turns the selectedCountry signal into a stream; takeUntilDestroyed unsubscribes when the page is destroyed.
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
// WHY: mat-card with header, title, subtitle and content for each dashboard section.
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
// WHY: The country dropdown of the job-title section.
import { MatSelectModule } from '@angular/material/select';
// WHY: The two statistics tables.
import { MatTableModule } from '@angular/material/table';
// WHY: RxJS operators used in the constructor and loadOverview(); each is explained where it is used.
import { catchError, filter, forkJoin, of, switchMap, tap } from 'rxjs';

// WHY: The small CSS bar chart component and the item type it expects.
import { BarChart, BarChartItem } from '../../shared/bar-chart/bar-chart';
import { errorMessage } from '../../shared/error-message';
import { CountryHeadcount, CountrySalaryStats, JobTitleSalaryStats } from '../insights.model';
import { InsightsService } from '../insights.service';

/**
 * The insights dashboard. Answers the HR Manager's three questions: salary range per
 * country, average salary per job title in a country, and headcount per country.
 * Salaries are only ever compared within one currency.
 */
// INTERVIEW: There is no "global average salary" card on purpose: averaging INR and USD together would be a meaningless number.
@Component({
  // WHY: The page is shown by the router (lazy-loaded), not by this tag.
  selector: 'app-insights-page',
  // WHY: Everything the template uses, including our own BarChart component.
  imports: [
    DecimalPipe, BarChart, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule,
    MatProgressBarModule, MatSelectModule, MatTableModule
  ],
  templateUrl: './insights-page.html',
  styleUrl: './insights-page.scss'
})
export class InsightsPage {
  private readonly insightsService = inject(InsightsService);
  // WHY: Needed by formatNumber/formatCurrency so chart labels use the same number format as the pipes.
  private readonly locale = inject(LOCALE_ID);

  // WHY: Column order of each table; names must match the matColumnDef names in the template.
  protected readonly countryColumns = ['country', 'employees', 'min', 'average', 'max'];
  protected readonly jobTitleColumns = ['jobTitle', 'employees', 'min', 'average', 'max'];

  // WHY: Overview state: the two server results plus loading and error flags for the whole page.
  protected readonly countryStats = signal<CountrySalaryStats[]>([]);
  protected readonly headcount = signal<CountryHeadcount[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  // WHY: The country chosen in the job-title section; '' means "not chosen yet" (it is set to the largest country after loading).
  protected readonly selectedCountry = signal('');
  // WHY: Separate state for the job-title section, so its loading/error does not hide the rest of the dashboard.
  protected readonly jobTitleStats = signal<JobTitleSalaryStats[]>([]);
  protected readonly jobTitlesLoading = signal(false);
  protected readonly jobTitlesError = signal<string | null>(null);

  // WHY: Total employees = sum of headcounts; reduce adds the counts starting from 0. Counts are safe to add across countries (no currency).
  protected readonly totalEmployees = computed(() =>
    this.headcount().reduce((sum, row) => sum + row.employeeCount, 0)
  );
  // WHY: Headcount is sorted largest first, so the first row is the largest team; null when there is no data.
  protected readonly largestCountry = computed(() => this.headcount()[0] ?? null);
  // WHY: Country names A–Z for the dropdown; map creates a new array, so sort() does not change the headcount signal's array.
  protected readonly countries = computed(() => this.headcount().map(row => row.country).sort());

  // WHY: Converts headcount rows into the generic bar chart format; computed, so it updates when headcount changes.
  protected readonly headcountChart = computed<BarChartItem[]>(() =>
    this.headcount().map(row => ({
      label: row.country,
      value: row.employeeCount,
      // WHY: formatNumber adds thousands separators, like the number pipe in the template.
      valueLabel: formatNumber(row.employeeCount, this.locale)
    }))
  );

  // WHY: Bars of average salary per job title; all rows are in the same country, so one currency, so the bars are comparable.
  protected readonly jobTitleChart = computed<BarChartItem[]>(() =>
    this.jobTitleStats().map(row => ({
      label: row.jobTitle,
      value: row.averageSalary,
      valueLabel: this.money(row.averageSalary, row.currency)
    }))
  );

  constructor() {
    // WHY: Load the summary cards, the country table and the headcount chart as soon as the page opens.
    this.loadOverview();

    // WHY: Each time selectedCountry changes, load the job-title statistics for that country.
    toObservable(this.selectedCountry)
      .pipe(
        // WHY: filter skips the empty start value, so no request is sent before a country is chosen.
        filter(country => country !== ''),
        // WHY: tap shows this section's progress bar and clears its old error before the request.
        tap(() => {
          this.jobTitlesLoading.set(true);
          this.jobTitlesError.set(null);
        }),
        // WHY: switchMap cancels the request for the previous country if the user switches quickly, so stale data is never shown.
        switchMap(country =>
          this.insightsService.salaryByJobTitle(country).pipe(
            // WHY: Inside switchMap, so an error only affects this request; the stream keeps working for the next country.
            catchError(error => {
              this.jobTitlesError.set(errorMessage(error));
              // WHY: Replace the error with an empty list, so the subscribe below still runs and stops the loading state.
              return of([]);
            })
          )
        ),
        // WHY: Stop listening when the user leaves the page (no memory leak).
        takeUntilDestroyed()
      )
      .subscribe(stats => {
        this.jobTitleStats.set(stats);
        this.jobTitlesLoading.set(false);
      });
  }

  /** Loads the per-country statistics and headcount together; picks the largest country. */
  // WHY: protected (not private) because the "Try again" button in the template calls it.
  protected loadOverview(): void {
    this.loading.set(true);
    this.error.set(null);
    // WHY: forkJoin runs both requests in parallel and emits once, when BOTH have finished, so the page renders once with complete data.
    // INTERVIEW: forkJoin fits here because each HTTP call emits one value and completes; if either fails, the whole overview shows one error.
    forkJoin({
      stats: this.insightsService.salaryByCountry(),
      headcount: this.insightsService.headcountByCountry()
    }).subscribe({
      // WHY: The result is an object with the same keys; destructuring ({ stats, headcount }) names the two results.
      next: ({ stats, headcount }) => {
        this.countryStats.set(stats);
        this.headcount.set(headcount);
        this.loading.set(false);
        // WHY: Pick the largest country by default, but only the first time (keep the user's choice on "Try again").
        if (!this.selectedCountry() && headcount.length > 0) {
          // WHY: Setting the signal triggers the job-title stream in the constructor.
          this.selectedCountry.set(headcount[0].country);
        }
      },
      // WHY: Show the error card with a "Try again" button.
      error: error => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      }
    });
  }

  /** Formats an amount with its currency code, e.g. "INR 1,466,667". */
  // WHY: Shows the currency CODE (not a symbol) because "$" is ambiguous (USD, CAD, AUD, SGD); '1.0-0' = no decimals, enough for a summary.
  protected money(value: number, currency: string): string {
    return formatCurrency(value, this.locale, `${currency} `, currency, '1.0-0');
  }
}
