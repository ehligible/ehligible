# Glossary

| Term | Meaning |
|---|---|
| **Application date (A)** | The date the applicant signs the application. Not counted itself. |
| **Eligibility window** | `[A − 5 years, A − 1]`, 1,826 or 1,827 calendar days. |
| **Present day** | A calendar day on which the person was in Canada at any time. |
| **Absence day** | A full calendar day outside Canada — strictly between a departure date and a return date. |
| **Trip** | A departure date and a return date (both present days). Same-day/next-day trips have 0 absence days but must still be declared. |
| **Planned trip** | A future trip the user adds to see how it moves their dates. |
| **PR date (P)** | The day the person became a permanent resident. Counts as a PR day. |
| **PR day** | A present day on or after the PR date. Worth 1. |
| **Status day (NPR day)** | A present day before the PR date while holding temporary resident status or protected person status. Worth ½, capped at 365 credited days. |
| **Credited days** | `PR days + min(status days / 2, 365)`. Need ≥ 1,095. |
| **First eligible date** | The earliest A ≥ today with ≥ 1,095 credited days, assuming presence on future days except planned trips. What the widget counts down to. |
| **Safer apply date** | The earliest A ≥ today with ≥ 1,095 + margin credited days (margin default 7). |
| **Check** | One detection attempt producing `IN`, `OUT(country)`, `BORDER`, or `BORDER_FOREIGN`. |
| **Day verdict** | present · absent · assumed present · needs review (counted absent until confirmed). |
| **Tracking start** | The day automatic detection began (install/onboarding). Earlier days rely on entered trips. |
