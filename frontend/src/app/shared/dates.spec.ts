// STUDY NOTES: Checks the two date helpers that convert between a JavaScript Date and the "YYYY-MM-DD" text
// STUDY NOTES: the backend uses for LocalDate. Plain UNIT test: pure functions, so no TestBed is needed.
// STUDY NOTES: It guards a classic bug: toISOString() works in UTC and can move the date by one day.
import { parseIsoDate, toIsoDate } from './dates';

// WHY: describe groups related tests under one name in the test report.
describe('date helpers', () => {
  // WHY: Proves the hire date the user picks is the date that is sent, in any time zone.
  it('formats the local calendar day without a time-zone shift', () => {
    // WHY: JavaScript months start at 0, so 3 = April. 23:30 is late at night: in a time zone ahead
    // WHY: of UTC, toISOString() would give the wrong day. Our helper must still say 2022-04-01.
    expect(toIsoDate(new Date(2022, 3, 1, 23, 30))).toBe('2022-04-01');
    // WHY: Just after midnight: the other edge. Also proves month and day are padded with a 0 ("01", "05").
    expect(toIsoDate(new Date(2022, 0, 5, 0, 15))).toBe('2022-01-05');
  });

  // WHY: Proves the reverse direction (used when editing an existing employee) and that a
  // WHY: round trip text -> Date -> text gives the same value back.
  it('parses an ISO date to local midnight and back', () => {
    // WHY: Act: 31 December is a good edge case (last day of month and year).
    const date = parseIsoDate('2021-12-31');

    // WHY: Assert each part in LOCAL time. getMonth() is 0-based, so December = 11.
    expect(date.getFullYear()).toBe(2021);
    expect(date.getMonth()).toBe(11);
    expect(date.getDate()).toBe(31);
    // WHY: Round trip: formatting the parsed date gives the original text.
    expect(toIsoDate(date)).toBe('2021-12-31');
  });
});
