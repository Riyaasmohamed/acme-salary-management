// STUDY NOTES: A tiny reusable horizontal bar chart component used on the insights page (headcount and average salary by job title).
// STUDY NOTES: It is built with CSS only, because the dashboard needs simple bars and a chart library would add size and licence questions.
// WHY: input() declares component inputs as signals; computed() creates a value derived from other signals.
import { Component, computed, input } from '@angular/core';

/**
 * One bar: the label on the left, the bar length from `value`, the formatted text on the right.
 */
// WHY: value (a number) decides the bar length; valueLabel is the already formatted text (e.g. "INR 1,830,243"), so the chart does not need to know about money.
export interface BarChartItem {
  label: string;
  value: number;
  valueLabel: string;
}

/**
 * A simple horizontal bar chart drawn with CSS. The longest bar is the largest value; the
 * others are scaled relative to it. No chart library is needed for this.
 */
// INTERVIEW: No chart library on purpose: about 30 lines of code, no extra bundle size, easy to unit test (see bar-chart.spec.ts).
@Component({
  // WHY: Used in the insights page template as <app-bar-chart [items]="..." label="..." />.
  selector: 'app-bar-chart',
  // WHY: No "imports" array is needed because the template only uses built-in syntax (@for, bindings).
  templateUrl: './bar-chart.html',
  styleUrl: './bar-chart.scss'
})
export class BarChart {
  // WHY: input.required means the parent must pass items; it is a signal, so we read it as this.items().
  readonly items = input.required<BarChartItem[]>();
  // WHY: Optional input with a default text, used as the accessible name of the chart.
  readonly label = input('Bar chart');

  // WHY: computed() recalculates the largest value only when items() changes, not on every bar; Math.max(0, ...) returns 0 for an empty list.
  private readonly maxValue = computed(() => Math.max(0, ...this.items().map(item => item.value)));

  /** Width of a bar as a percentage of the largest value. */
  // WHY: protected so the template can call it, but other classes cannot.
  protected widthPercent(item: BarChartItem): number {
    const max = this.maxValue();
    // WHY: The largest bar is 100%, others are scaled to it; the check avoids dividing by zero when all values are 0.
    return max > 0 ? (item.value / max) * 100 : 0;
  }
}
