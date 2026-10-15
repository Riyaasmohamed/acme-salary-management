// STUDY NOTES: TypeScript types for the three insight responses from /api/insights. They mirror the backend's response records.
// STUDY NOTES: All numbers are already computed by the database (MIN, MAX, AVG, COUNT ... GROUP BY); the UI only formats them.
/**
 * Salary statistics for one country, in that country's currency.
 */
// WHY: Grouped by country AND currency, so min/max/average are always in one real currency (never mixed INR and USD).
export interface CountrySalaryStats {
  country: string;
  // WHY: The currency code is needed to format every amount in this row correctly.
  currency: string;
  minSalary: number;
  maxSalary: number;
  // WHY: Already rounded to two decimals by the backend.
  averageSalary: number;
  employeeCount: number;
}

/**
 * Salary statistics for one job title inside a country.
 */
// WHY: Used for the "average salary by job title" chart and table of one selected country.
export interface JobTitleSalaryStats {
  jobTitle: string;
  currency: string;
  minSalary: number;
  maxSalary: number;
  averageSalary: number;
  employeeCount: number;
}

/**
 * Number of employees in one country.
 */
// WHY: Headcount does not depend on currency, so it can be added up into a total across countries.
export interface CountryHeadcount {
  country: string;
  employeeCount: number;
}
