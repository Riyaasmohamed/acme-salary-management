// STUDY NOTES: Checks the BarChart component: bar widths are scaled to the largest value, the value text is
// STUDY NOTES: shown, and an empty list shows a message. COMPONENT test with TestBed: the chart is pure CSS,
// STUDY NOTES: so the only way to test it is to render the template and read the DOM (style.width, text).
import { TestBed } from '@angular/core/testing';

import { BarChart } from './bar-chart';

describe('BarChart', () => {
  // WHY: Helper so each test is one line of setup: give items in, get the rendered DOM out.
  function render(items: { label: string; value: number; valueLabel: string }[]): HTMLElement {
    // WHY: No configureTestingModule needed: BarChart is standalone and has no dependencies.
    const fixture = TestBed.createComponent(BarChart);
    // WHY: "items" is a required signal input (input.required). setInput sets it the same way a parent
    // WHY: template would with [items]="...". Without it, reading items() would throw.
    fixture.componentRef.setInput('items', items);
    // WHY: Render the template with the new input.
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  // WHY: Proves the scaling math: largest value = 100% width, others relative to it.
  it('scales every bar relative to the largest value', () => {
    // WHY: Arrange + Act: 200 is the max, 50 is a quarter of it.
    const element = render([
      { label: 'India', value: 200, valueLabel: '200' },
      { label: 'Japan', value: 50, valueLabel: '50' }
    ]);

    // WHY: Read the inline width Angular set via [style.width.%] on each .bar element.
    const widths = Array.from(element.querySelectorAll<HTMLElement>('.bar')).map(bar => bar.style.width);
    // WHY: 200/200 = 100% and 50/200 = 25%, in the same order as the input.
    expect(widths).toEqual(['100%', '25%']);
    // WHY: The formatted text (valueLabel) is shown next to the first bar.
    expect(element.querySelector('.value')?.textContent).toBe('200');
  });

  // WHY: Empty state: with no data the user sees a message instead of a blank box.
  // WHY: This also covers the @empty block of the @for loop in the template.
  it('shows a message when there is nothing to draw', () => {
    expect(render([]).textContent).toContain('No data to show');
  });
});
