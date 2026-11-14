# EventSphere – Smart Campus Event Management & Student Participation Tracking System

A console-based Java application that lets a college manage technical and cultural fest events from one place: event creation, student registration, attendance, certificates, participation profiles and analytics.

## Features

| Module | What it does |
|---|---|
| Event Management | Admin/organiser creates, edits and cancels events with date, venue, category, registration deadline and participant limit |
| Student Registration | Browse/filter events, register individually or as a team, get a registration ID, see Confirmed/Waitlisted status, cancel registration, duplicate registration blocked, automatic waitlist promotion |
| Attendance Management | Organiser marks attendance using registration IDs, duplicate check-ins blocked, attendance report per event |
| Certificate Generation | Personalised certificate with name, event, date and unique certificate ID; public verification by certificate ID |
| Student Participation Profile | Events registered/attended, competitions, certificates earned, attendance rate, history |
| Analytics & Reports | Overall totals, per-event statistics, registrations by category |

## User Roles

- **Admin** – approves organisers, manages all events, views registrations and analytics
- **Event Organiser** – creates own events, views registered students, marks attendance, issues certificates
- **Student** – creates account, registers for events, tracks status, views certificates and profile

## Tech Stack

- Java 11+ (core Java, Collections, Streams, Serialization, File I/O)
- No external libraries

## How to Run

```bash
javac EventSphere.java
java EventSphere
```

Default admin login: `admin@campus.com` / `admin123`

## Suggested Demo Flow

1. Register an **Organiser** account → log in as Admin and approve it
2. Log in as Organiser → create an event (set a small participant limit, e.g. 2)
3. Register 3 **Student** accounts → register all for the event (the third is waitlisted)
4. Cancel one confirmed registration → the waitlisted student is promoted automatically
5. As Organiser, mark attendance with registration IDs (try a duplicate check-in)
6. Issue certificates (event date must be today or earlier) → files appear in `certificates/`
7. Verify a certificate from the main menu, view the student profile and admin analytics

## Data Storage

Data is saved automatically to `eventsphere.dat` (Java serialization). Certificates are written to the `certificates/` folder as text files.

## Future Enhancements

- Swing/JavaFX GUI or Spring Boot web version
- MySQL database using JDBC
- QR-code based check-in and PDF certificates
- Email notifications and password hashing

## Author

Divyanshi Singh – CGC University Mohali
