#!/usr/bin/env bash
# M01 curl suite: Foundation (settings and health). Each case names the acceptance id it proves.
# Runs in order on an empty database: AC-M01-01 must run before the PUT in AC-M01-02.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

case_begin "AC-M01-01 GET /api/settings on empty db returns defaults with configured=false"
req GET /api/settings
expect_status 200
expect_json '.configured == false'
expect_json '.timeZone == "UTC"'
expect_json '.notificationsEnabled == true'
expect_json '.defaultStartPage == "DASHBOARD"'
case_end

case_begin "AC-M01-02 PUT valid settings returns 200, then GET shows them with configured=true"
req PUT /api/settings '{"displayName":"Alex","email":"alex@example.com","timeZone":"Europe/Berlin","notificationsEnabled":false,"defaultStartPage":"TASKS"}'
expect_status 200
expect_json '.configured == true'
req GET /api/settings
expect_status 200
expect_json '.displayName == "Alex"'
expect_json '.email == "alex@example.com"'
expect_json '.timeZone == "Europe/Berlin"'
expect_json '.notificationsEnabled == false'
expect_json '.defaultStartPage == "TASKS"'
expect_json '.configured == true'
case_end

case_begin "AC-M01-03 invalid time zone returns 400 with fieldErrors[0].field = timeZone"
req PUT /api/settings '{"displayName":"Alex","email":null,"timeZone":"Mars/Olympus","notificationsEnabled":true,"defaultStartPage":"DASHBOARD"}'
expect_status 400
expect_json '.status == 400'
expect_json '.fieldErrors[0].field == "timeZone"'
case_end

case_begin "AC-M01-04 invalid defaultStartPage returns 400"
req PUT /api/settings '{"displayName":"Alex","email":null,"timeZone":"UTC","notificationsEnabled":true,"defaultStartPage":"CALENDAR"}'
expect_status 400
expect_json '.status == 400'
case_end

case_begin "AC-M01-05 blank displayName returns 400 with fieldErrors[0].field = displayName"
req PUT /api/settings '{"displayName":"   ","email":null,"timeZone":"UTC","notificationsEnabled":true,"defaultStartPage":"DASHBOARD"}'
expect_status 400
expect_json '.fieldErrors[0].field == "displayName"'
case_end

case_begin "AC-M01-06 GET /actuator/health returns status UP"
req GET /actuator/health
expect_status 200
expect_json '.status == "UP"'
case_end

suite_end
