# Canadian citizenship grant (s. 5(1)) — counting rules

Researched 2026-10-29 from primary sources only (Citizenship Act, IRCC program delivery instructions,
IRCC's physical presence calculator + FAQ, CIT 0407, canada.ca). This document is the authority for the
rules engine in `core/`. Every rule implemented there should trace back to a section here.

> The old CIT 0002 guide URL now 404s; IRCC replaced it with the web pages cited as [WHO], [HOW], [PP].

## 1. Physical presence for adults

- Law: s. 5(1)(c)(i) requires that the person "has, since becoming a permanent resident, been physically
  present in Canada for at least 1,095 days during the five years immediately before the date of his or
  her application" [ACT].
- **Application date = date the applicant signs** (IRCC policy; the Act/Regulations don't define it).
  - [PDI]: "within the five years immediately preceding the date the applicant signed the application".
  - [WHO]: the eligibility period "is 5 years before the date you sign your application".
  - [HOW] (paper): "the date of signature on the application form is the same as the "Application date" in
    the physical presence calculation"; "We'll return your application to you if we receive it more than 90
    days after the date on the form."
  - [CALC A15]: "The date you sign cannot be in the future".
- **The application day itself is outside the window.**
  - [PDI]: applicants "must meet the residence/physical presence requirements the day before signing their
    application. The day the applicant applies is not counted towards residence/physical presence."
  - [0407]: "Do not count the actual date you will sign your application in your calculation".
- **Window is calendar-date based.** [0407] example: applying April 1, 2018 → period "April 1, 2013 to
  March 31, 2018"; "April 1, 2013 can be counted"; "Enter 1826 in Box C"; "The maximum number of days you can
  enter in Box C is 1827." [PDI]: "February 29 (leap day) is counted in either presence or absence."
  → the window has 1,826 or 1,827 days.

## 2. Credit for time before becoming a PR

- Law: s. 5(1.001) — each day physically present "as a temporary resident or protected person … before
  becoming a permanent resident, the person accumulates half of a day of physical presence, up to a maximum
  of 365 days"; each day "since becoming a permanent resident" counts as one day [ACT].
- Qualifying temporary resident statuses: "lawful authorization to enter or remain in Canada as a: visitor;
  student; worker; or, temporary resident permit holder" [PDI].
- Protected persons: "found to be in need of protection or a convention refugee by the Immigration and
  Refugee Board" or positive PRRA [PDI]; counts only "from the time your claim or application was approved
  until 1 day before you became a permanent resident" [WHO].
- Refugee claimants: "time will not be credited in Canada between the date of the claim and the date the
  applicant received a positive decision" [PDI]. Work/study permits issued during a claim or PRRA "did not
  grant you Temporary Resident status" [CALC, 0407]. PDI Example 6: claim 2015-04-01, protected 2015-08-16 →
  ineligible "April 1, 2015 to August 15, 2015 (137 days)" (decision day is credited).
- Without status: "you cannot use anytime spent in Canada without the authorized status of temporary
  resident, protected person or permanent resident" [CALC A6].
- Maintained status: not addressed by citizenship sources. IRPR s. 183(5)–(6) says the temporary resident
  "retains their status" → counts (our inference). CIT 0407: if unsure of status for a period, "do NOT include
  that period". **App behaviour:** counts it, with an info note telling users to exclude periods they're unsure of.
- Same window: "Each day as a temporary resident or protected person during your eligibility period counts
  as 0.5 days" [WHO]; the 365 cap is "equal to 730 calendar days".
- **The PR landing day is a full PR day** (PDI Example 2: PR 2016-01-01 counts "01JAN2016 to 31MAR2016 = 91";
  pre-PR time ends 2015-12-31). [PDI]: "An applicant will always need at least 730 days of physical presence
  after becoming a permanent resident, when using the maximum allowable NPR physical presence credit."
- Rounding: never stated. PDI Example 1 shows "731 x 0.5 = 365.5" → "Maximum NPR = 365".

## 3. Partial days and trips

- [PDI]: "Absences will be calculated only for days where an applicant spent no time at all in Canada. Dates
  where an applicant left Canada, or returned to Canada will not be counted as an absence since the applicant
  was physically present in Canada for a portion of both days."
- [CALC A3]: "an absence between July 1, 2013 and July 15, 2013 equals 13 days of absence… if you leave Canada
  on July 1, 2013 and return on July 2, 2013, this equals 0 days of absence. You must still declare this as an
  absence. Think of it as counting only the full days outside of Canada."
- Same-day trips: [CALC A7] "Yes. List all absences, even if you come back the same day." [0407] "list all
  trips where you left and returned to Canada on the same day or next day, including day trips to the United
  States" → absent days = 0.
- Absences before PR must also be declared and deducted [CALC A10; 0407 p. 4].

## 4. Time that doesn't count / time abroad that does (out of scope for v1)

- s. 21: no period counts while "under a probation order", "a paroled inmate", or serving "a term of
  imprisonment" [ACT]; conditional sentences also excluded [C6]. Sentence day counting is inclusive
  [CALC A12–A13].
- Exceptions: probation from a successfully completed conditional discharge; completed youth sentences [CALC A11].
- Crown servants / CAF abroad (s. 5(1.01)–(1.03)) count as present, paper application only (CIT 0177);
  CAF members with 3 years' service in 6 years are exempt from 5(1)(c) (s. 5(1.2), CIT 0531).
- **App behaviour:** not supported in v1; the app tells these users to use IRCC's calculator.

## 5. Other requirements (checklist screen)

- PR status: "valid permanent resident (PR) status", no removal order, no unfulfilled conditions; "You can
  apply with a valid or expired PR card" [WHO].
- Taxes: returns (where required) for three taxation years "fully or partially within the five years
  immediately before the date of his or her application" [ACT]; "taxation years are the same as calendar
  years"; "The year the applicant submitted their application will generally not be counted" (June 1, 2020 →
  2015–2019) [TAX].
- Language: if "18 years of age or more but less than 55 years of age at the date of his or her application"
  [ACT]; speaking & listening at CLB/NCLC level 4 [WHO].
- Test: same ages (18–54 "on the day you sign"); 20 questions, 45 minutes, pass mark 15/20, 3 chances [TEST].
- Prohibitions (s. 22): probation/parole/prison; serving a sentence abroad; charged with/on trial for/appealing
  an indictable offence; convicted of an indictable offence in the 4 years before applying; misrepresentation
  refusal in the last 5 years; revocation in the last 10 years.
- Fees: adult $653 (processing + right of citizenship), minor $100; right-of-citizenship fee $123 for
  applications received on or after March 31, 2026; "updated annually based on inflation" [FEES, ROCF].
- Minors: 5(2) has no physical presence requirement; 5(1) minors need 1,095 days (out of scope for v1).

## 6. Changes 2024–2026

- Bill C-3 (S.C. 2025, c. 5): royal assent 2025-11-20, in force 2025-12-15. Its 1,095-day test is for
  *Canadian parents* (citizenship by descent); it does not touch s. 5(1)(c), 5(1.001), 21 or 22 [C3].
- No enacted or proposed change to the 1,095-days-in-5-years rule was found (also checked SOR/2025-278,
  Bill C-12 (2026), Bill C-274).
- Online application: "Most applicants can now apply online" [HOW]; the calculator is built into the online
  account [PP]. The public calculator is for paper applicants only [ACCT].

## 7. Margin and errors

- [WHO]: "We encourage you to apply with more than 1,095 days of living in Canada in case there's a problem
  with the calculation."
- Under-minimum applications "are returned… with a full fee refund" [PDI]; a miscalculation found during
  processing → "your application will be refused. Your citizenship processing fee will not be refunded"
  [CALC A15]. IRCC checks CBSA entry records [HC827].
- Projection semantics match IRCC's: "a date when you should be eligible to apply, assuming nothing changes
  and you remain in Canada" [CALC A16].

## Official worked examples (test vectors)

| # | Key dates | Result |
|---|---|---|
| 1 | TR from 2014-04-01, PR 2016-04-01, apply 2018-04-01 | 365 + 730 = 1,095 → eligible; applying 2018-03-31 → 1,094 |
| 2 | out-of-status period + a sentence | 1,127 → eligible |
| 3 | PR 2017-04-01, apply 2018-04-01 | 730 → not eligible |
| 4 | Example 2 + a 44-day trip as PR | 1,083 → not eligible |
| 5 | PR on arrival 2014-04-01, apply 2018-04-01 | 1,461 → eligible |
| 6 | refugee claim then protected person | 1,186 → eligible |

Known typos in the PDI (fix before using as test data): "365 + 366 + 275 = 1005" (second figure should be
365); Examples 4 and 6 say "PR = April 1, 2016" but their arithmetic uses January 1, 2016; calculator FAQ A2's
worked example overlaps TR and PR time — don't use it.

## Algorithm (as implemented)

- Let A = signing date, P = PR date, S = same month/day five years before A (if A is Feb 29, use Mar 1 —
  the conservative choice). Window W = [S, A−1] inclusive.
- Day d is **present** unless some trip (L = departure, R = return) has L < d < R. Departure/return days are
  present; same-day and next-day trips remove nothing.
- **PR days** = present days d in W with d ≥ P.
- **NPR days** = present days d in W with d < P inside a status period (temporary resident or protected person
  from approval), excluding refugee-claim waiting periods and no-status periods.
- **Eligible on A** iff `2·PR + min(NPR, 730) ≥ 2,190` (exact; never round up). Display
  `PR + min(NPR/2, 365)` floored.
- **Countdown target** = smallest A ≥ today that is eligible, assuming presence on all future days except
  planned trips. Step one day at a time (old trips and pre-PR days roll out of the window).
- Gates at A: age 18–54 → language + test; ≥ 3 tax years among calendar years overlapping W excluding A's
  year; PR status valid; paper applications received within 90 days of A.

## Open uncertainties

1. Online application date when signing and submitting differ (group applications).
2. Window start when A is Feb 29 (we use Mar 1, conservative).
3. Maintained status (we count it, with a note).
4. A sentence day that's also an absence (out of scope).
5. How the official calculator displays half days (only matters if IRCC rounds up; we never do).
6. Time zones for departure/arrival dates (we use the local calendar date where the phone was).
7. Previous year's tax return before it's due (e.g., applying in February).
8. The PDI is dated 2021-01-04; the calculator FAQ (2026-07-17) agrees with it.
9. Fees change every March 31 — re-check [FEES] yearly.

## Sources

- ACT — Citizenship Act s. 5, 21, 22: https://laws-lois.justice.gc.ca/eng/acts/c-29/section-5.html (current to 2026-09-30)
- PDI — How residence/physical presence is calculated: https://www.canada.ca/en/immigration-refugees-citizenship/corporate/publications-manuals/operational-bulletins-manuals/canadian-citizenship/grant/residence/calculate-physical-presence.html (2021-01-04)
- CALC — Physical presence calculator + FAQ: https://eservices.cic.gc.ca/rescalc/resCalcStartNew.do?lang=en, https://eservices.cic.gc.ca/rescalc/redir.do?redir=faq (2026-07-17)
- 0407 — CIT 0407: https://www.canada.ca/content/dam/ircc/documents/pdf/english/kits/forms/cit0407/01-05-2019/cit0407e.pdf
- WHO — https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-citizenship/adult-minor/who.html (2026-04-21)
- HOW — https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-citizenship/adult-minor/how.html (2026-04-21)
- PP — https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-citizenship/adult-minor/how/physical-presence.html (2025-09-11)
- ACCT — https://www.canada.ca/en/immigration-refugees-citizenship/services/application/ircc-accounts.html (2026-09-01)
- HC1520 / HC827 — https://ircc.canada.ca/english/helpcentre/answer.asp?qnum=1520&top=5 (2026-09-28)
- TAX — …/canadian-citizenship/grant/residence/income-tax-filing.html (2017-10-16)
- C6 — https://www.canada.ca/en/immigration-refugees-citizenship/news/2018/02/changes_to_the_citizenshipactasaresultofbillc-6.html
- FEES — https://ircc.canada.ca/english/information/fees/fees.asp (2026-09-21)
- ROCF — https://www.canada.ca/en/immigration-refugees-citizenship/news/notices/right-citizenship-fee-increasing.html (2026-03-27)
- TEST — https://www.canada.ca/en/immigration-refugees-citizenship/services/canadian-citizenship/test/study.html (2026-03-31)
- C3 — https://www.parl.ca/legisinfo/en/bill/45-1/c-3 ; https://www.canada.ca/en/immigration-refugees-citizenship/news/2025/12/bill-c-3-an-act-to-amend-the-citizenship-act-2025-comes-into-effect.html
- IRPR s. 183 — https://laws-lois.justice.gc.ca/eng/regulations/SOR-2002-227/section-183.html
