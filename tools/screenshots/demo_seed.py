"""Writes demo data for store screenshots (fictional person) to the path given as the first argument.
An optional second argument "fr" gives the trips French reasons.

PR since 2025-03-01, status (student) since 2024-09-01, a past Paris trip, a planned Mexico trip, and
detection logs since 2026-09-01 with a detected overnight US trip and a same-day US trip.
The last check is "in Canada, just now" so the status chip shows In Canada.
"""
import json
import sys
import time
from datetime import date, timedelta

today = date.today()
french = len(sys.argv) > 2 and sys.argv[2] == "fr"
reason = {"Vacation": "Vacances", "Wedding": "Mariage", "Hiking": "Randonnée"} if french else {}
start = date(2026, 9, 1)
logs = {}
day = start
while day <= today:
    logs[day.isoformat()] = {"inside": 3, "first": "INSIDE", "last": "INSIDE"}
    day += timedelta(days=1)
logs["2026-09-20"] = {"inside": 2, "outside": 1, "first": "INSIDE", "last": "INSIDE", "countries": ["US"]}
logs["2026-10-10"] = {"inside": 1, "outside": 1, "first": "INSIDE", "last": "OUTSIDE", "countries": ["US"]}
logs["2026-10-11"] = {"outside": 3, "first": "OUTSIDE", "last": "OUTSIDE", "countries": ["US"]}
logs["2026-10-12"] = {"inside": 1, "outside": 1, "first": "OUTSIDE", "last": "INSIDE", "countries": ["US"]}

data = {
    "schemaVersion": 1,
    "profile": {
        "prDate": "2025-03-01",
        "prePrStatus": {"firstStatusDate": "2024-09-01"},
        "trackingStart": start.isoformat(),
        "birthYear": 1994,
    },
    "trips": [
        {"id": "paris", "departure": "2025-07-01", "returnDate": "2025-07-20", "destination": "Paris, France", "reason": reason.get("Vacation", "Vacation")},
        {"id": "mexico", "departure": "2027-02-10", "returnDate": "2027-02-24", "destination": "Mexico City", "reason": reason.get("Wedding", "Wedding")},
    ],
    "dayLogs": logs,
    "tripNotes": {"2026-10-10": {"destination": "Burlington, Vermont", "reason": reason.get("Hiking", "Hiking")}},
    "settings": {"marginDays": 7, "countdownTarget": "EXACT", "notificationsEnabled": True, "checklist": ["pr", "taxes"]},
    "lastCheck": {"epochMillis": int(time.time() * 1000), "kind": "INSIDE"},
    "celebrated": ["QUARTER", "HALFWAY", "THREE_QUARTERS", "DAYS_LEFT_100", "DAYS_LEFT_30", "DAYS_LEFT_7", "DAYS_LEFT_1", "ELIGIBLE"],
}
with open(sys.argv[1], "w", encoding="utf-8") as f:
    json.dump(data, f)
print("demo data written for", today)
