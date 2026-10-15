// STUDY NOTES: Two small helpers to convert between a JavaScript Date and the "YYYY-MM-DD" text the backend uses for hireDate (a Java LocalDate).
// STUDY NOTES: They exist because the built-in toISOString() works in UTC and can move the date one day back for users east of UTC (e.g. India).
/**
 * Formats a date as "YYYY-MM-DD" using the local calendar day. `toISOString()` is not used
 * because it converts to UTC and can shift the date by one day.
 */
// INTERVIEW: A hire date is a calendar day, not a moment in time, so it must never go through a time-zone conversion.
export function toIsoDate(date: Date): string {
  // WHY: getMonth() is zero-based (January = 0), so we add 1; padStart makes "3" into "03".
  const month = String(date.getMonth() + 1).padStart(2, '0');
  // WHY: getDate() returns the local day of the month; padded to two digits for the YYYY-MM-DD format.
  const day = String(date.getDate()).padStart(2, '0');
  // WHY: A template string (backticks) joins the parts into the exact format the backend expects.
  return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * Parses "YYYY-MM-DD" into a local date at midnight (no time-zone shift).
 */
export function parseIsoDate(value: string): Date {
  // WHY: split('-') gives ["2024","03","15"]; map(Number) turns each part into a number; destructuring names them.
  const [year, month, day] = value.split('-').map(Number);
  // WHY: new Date(y, m, d) creates a LOCAL date (unlike new Date("2024-03-15"), which is read as UTC); month - 1 because months are zero-based.
  return new Date(year, month - 1, day);
}
