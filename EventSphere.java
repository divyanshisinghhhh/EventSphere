import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EventSphere - Smart Campus Event Management & Student Participation Tracking System
 * Console-based Java application (no external libraries).
 *
 * Modules: Event Management, Student Registration, Attendance Management,
 *          Certificate Generation, Student Participation Profile, Analytics & Reports.
 * Roles:   Admin, Event Organiser, Student.
 *
 * Compile: javac EventSphere.java
 * Run:     java EventSphere
 * Default admin login: admin@campus.com / admin123
 */
public class EventSphere {

    // ===================== MODELS =====================
    enum Role { ADMIN, ORGANISER, STUDENT }
    enum Status { CONFIRMED, WAITLISTED, CANCELLED }

    static class User implements Serializable {
        int id; String name, email, password; Role role; boolean approved;
        User(int id, String name, String email, String password, Role role, boolean approved) {
            this.id = id; this.name = name; this.email = email;
            this.password = password; this.role = role; this.approved = approved;
        }
    }

    static class Event implements Serializable {
        int id, organiserId, maxParticipants;
        String title, description, venue, category;
        LocalDate date, deadline;
        boolean cancelled;
    }

    static class Registration implements Serializable {
        String id; int eventId, studentId;
        Status status; boolean attended; String certId, teamName;
        LocalDate registeredOn = LocalDate.now();
    }

    static class Database implements Serializable {
        List<User> users = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<Registration> regs = new ArrayList<>();
        int userSeq = 1, eventSeq = 1, regSeq = 1000, certSeq = 1000;
    }

    // ===================== STATE =====================
    static final String DB_FILE = "eventsphere.dat";
    static final String CERT_DIR = "certificates";
    static Database db;
    static Scanner sc = new Scanner(System.in);
    static User current;

    public static void main(String[] args) {
        load();
        System.out.println("=====================================");
        System.out.println("  EventSphere - Smart Campus Events");
        System.out.println("=====================================");
        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Login");
            System.out.println("2. Register (Student / Organiser)");
            System.out.println("3. Verify Certificate");
            System.out.println("0. Exit");
            int c = readInt("Choice: ");
            switch (c) {
                case 1: login(); break;
                case 2: register(); break;
                case 3: verifyCertificate(); break;
                case 0: save(); System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // ===================== PERSISTENCE =====================
    static void load() {
        File f = new File(DB_FILE);
        if (f.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                db = (Database) in.readObject();
                return;
            } catch (Exception e) {
                System.out.println("Could not read saved data, starting fresh.");
            }
        }
        db = new Database();
        db.users.add(new User(db.userSeq++, "Administrator", "admin@campus.com", "admin123", Role.ADMIN, true));
        save();
    }

    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DB_FILE))) {
            out.writeObject(db);
        } catch (IOException e) {
            System.out.println("Warning: failed to save data: " + e.getMessage());
        }
    }

    // ===================== INPUT HELPERS =====================
    static String readLine(String p) { System.out.print(p); return sc.nextLine().trim(); }

    static int readInt(String p) {
        while (true) {
            try { return Integer.parseInt(readLine(p)); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    static LocalDate readDate(String p) {
        while (true) {
            try { return LocalDate.parse(readLine(p + " (YYYY-MM-DD): ")); }
            catch (Exception e) { System.out.println("Invalid date format."); }
        }
    }

    // ===================== AUTH =====================
    static void register() {
        System.out.println("\n--- REGISTER ---");
        System.out.println("1. Student\n2. Event Organiser (needs admin approval)");
        int r = readInt("Role: ");
        if (r != 1 && r != 2) { System.out.println("Invalid role."); return; }
        String name = readLine("Name: ");
        String email = readLine("Email: ").toLowerCase();
        if (name.isEmpty() || !email.contains("@")) { System.out.println("Invalid name/email."); return; }
        if (findUserByEmail(email) != null) { System.out.println("Email already registered."); return; }
        String pw = readLine("Password: ");
        if (pw.length() < 4) { System.out.println("Password must be at least 4 characters."); return; }
        Role role = r == 1 ? Role.STUDENT : Role.ORGANISER;
        db.users.add(new User(db.userSeq++, name, email, pw, role, role == Role.STUDENT));
        save();
        System.out.println(role == Role.STUDENT
                ? "Account created. You can log in now."
                : "Account created. Wait for admin approval before logging in.");
    }

    static void login() {
        String email = readLine("Email: ").toLowerCase();
        String pw = readLine("Password: ");
        User u = findUserByEmail(email);
        if (u == null || !u.password.equals(pw)) { System.out.println("Invalid credentials."); return; }
        if (!u.approved) { System.out.println("Your account is pending admin approval."); return; }
        current = u;
        System.out.println("\nWelcome, " + u.name + " (" + u.role + ")");
        switch (u.role) {
            case ADMIN: adminMenu(); break;
            case ORGANISER: organiserMenu(); break;
            default: studentMenu();
        }
        current = null;
    }

    static User findUserByEmail(String e) {
        return db.users.stream().filter(u -> u.email.equals(e)).findFirst().orElse(null);
    }

    static User findUser(int id) {
        return db.users.stream().filter(u -> u.id == id).findFirst().orElse(null);
    }

    static Event findEvent(int id) {
        return db.events.stream().filter(e -> e.id == id).findFirst().orElse(null);
    }

    static Registration findReg(String id) {
        return db.regs.stream().filter(r -> r.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    // ===================== ADMIN =====================
    static void adminMenu() {
        while (true) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. Approve event organisers");
            System.out.println("2. View all events");
            System.out.println("3. Create event");
            System.out.println("4. Edit / cancel event");
            System.out.println("5. View registrations for an event");
            System.out.println("6. Analytics & reports");
            System.out.println("0. Logout");
            switch (readInt("Choice: ")) {
                case 1: approveOrganisers(); break;
                case 2: listEvents(db.events, true); break;
                case 3: createEvent(); break;
                case 4: editOrCancelEvent(false); break;
                case 5: viewEventRegistrations(false); break;
                case 6: analytics(); break;
                case 0: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void approveOrganisers() {
        List<User> pending = db.users.stream()
                .filter(u -> u.role == Role.ORGANISER && !u.approved).collect(Collectors.toList());
        if (pending.isEmpty()) { System.out.println("No pending organisers."); return; }
        for (User u : pending) System.out.println(u.id + ". " + u.name + " <" + u.email + ">");
        int id = readInt("Enter user ID to approve (0 to cancel): ");
        for (User u : pending) {
            if (u.id == id) { u.approved = true; save(); System.out.println("Approved " + u.name); return; }
        }
        if (id != 0) System.out.println("Not found.");
    }

    // ===================== MODULE 1: EVENT MANAGEMENT =====================
    static void createEvent() {
        Event e = new Event();
        e.id = db.eventSeq++;
        e.organiserId = current.id;
        e.title = readLine("Title: ");
        e.description = readLine("Description: ");
        e.category = readLine("Category (Coding/Hackathon/Workshop/Dance/Debate/Cultural...): ");
        e.venue = readLine("Venue: ");
        e.date = readDate("Event date");
        e.deadline = readDate("Registration deadline");
        if (e.deadline.isAfter(e.date)) {
            System.out.println("Deadline cannot be after event date. Event not created.");
            db.eventSeq--; return;
        }
        e.maxParticipants = readInt("Max participants: ");
        if (e.maxParticipants <= 0 || e.title.isEmpty()) {
            System.out.println("Invalid data. Event not created.");
            db.eventSeq--; return;
        }
        db.events.add(e);
        save();
        System.out.println("Event created with ID " + e.id);
    }

    static void editOrCancelEvent(boolean ownOnly) {
        listEvents(visibleEvents(ownOnly), true);
        int id = readInt("Event ID (0 to go back): ");
        if (id == 0) return;
        Event e = findEvent(id);
        if (e == null || (ownOnly && e.organiserId != current.id)) { System.out.println("Event not found."); return; }
        System.out.println("1. Edit details\n2. Cancel event");
        int c = readInt("Choice: ");
        if (c == 2) {
            e.cancelled = true;
            db.regs.stream().filter(r -> r.eventId == e.id && r.status != Status.CANCELLED)
                    .forEach(r -> r.status = Status.CANCELLED);
            save();
            System.out.println("Event cancelled; all registrations cancelled.");
        } else if (c == 1) {
            String v;
            v = readLine("Title [" + e.title + "] (blank = keep): "); if (!v.isEmpty()) e.title = v;
            v = readLine("Description (blank = keep): "); if (!v.isEmpty()) e.description = v;
            v = readLine("Venue [" + e.venue + "]: "); if (!v.isEmpty()) e.venue = v;
            v = readLine("Category [" + e.category + "]: "); if (!v.isEmpty()) e.category = v;
            v = readLine("New date YYYY-MM-DD (blank = keep): ");
            if (!v.isEmpty()) try { e.date = LocalDate.parse(v); } catch (Exception ex) { System.out.println("Bad date, kept old."); }
            v = readLine("New max participants (blank = keep): ");
            if (!v.isEmpty()) try {
                int m = Integer.parseInt(v);
                if (m > 0) { e.maxParticipants = m; promoteWaitlist(e); }
            } catch (Exception ex) { System.out.println("Bad number, kept old."); }
            save();
            System.out.println("Event updated.");
        }
    }

    static List<Event> visibleEvents(boolean ownOnly) {
        return ownOnly ? db.events.stream().filter(e -> e.organiserId == current.id).collect(Collectors.toList())
                       : db.events;
    }

    static String eventState(Event e) {
        if (e.cancelled) return "CANCELLED";
        if (e.date.isBefore(LocalDate.now())) return "COMPLETED";
        return "UPCOMING";
    }

    static void listEvents(List<Event> list, boolean showStats) {
        if (list.isEmpty()) { System.out.println("No events."); return; }
        System.out.println();
        for (Event e : list) {
            System.out.printf("[%d] %s  (%s | %s)%n", e.id, e.title, e.category, eventState(e));
            System.out.printf("     Date: %s  Venue: %s  Reg. deadline: %s%n", e.date, e.venue, e.deadline);
            if (showStats)
                System.out.printf("     Seats: %d/%d  Waitlist: %d%n", confirmedCount(e.id), e.maxParticipants, waitlistCount(e.id));
        }
    }

    static long confirmedCount(int eventId) {
        return db.regs.stream().filter(r -> r.eventId == eventId && r.status == Status.CONFIRMED).count();
    }

    static long waitlistCount(int eventId) {
        return db.regs.stream().filter(r -> r.eventId == eventId && r.status == Status.WAITLISTED).count();
    }

    // ===================== ORGANISER =====================
    static void organiserMenu() {
        while (true) {
            System.out.println("\n--- ORGANISER MENU ---");
            System.out.println("1. Create event");
            System.out.println("2. My events");
            System.out.println("3. Edit / cancel my event");
            System.out.println("4. View registered students");
            System.out.println("5. Mark attendance");
            System.out.println("6. Issue certificates");
            System.out.println("7. Attendance report");
            System.out.println("0. Logout");
            switch (readInt("Choice: ")) {
                case 1: createEvent(); break;
                case 2: listEvents(visibleEvents(true), true); break;
                case 3: editOrCancelEvent(true); break;
                case 4: viewEventRegistrations(true); break;
                case 5: markAttendance(); break;
                case 6: issueCertificates(); break;
                case 7: attendanceReport(); break;
                case 0: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static Event pickEvent(boolean ownOnly) {
        listEvents(visibleEvents(ownOnly), true);
        if (visibleEvents(ownOnly).isEmpty()) return null;
        int id = readInt("Event ID (0 to go back): ");
        if (id == 0) return null;
        Event e = findEvent(id);
        if (e == null || (ownOnly && e.organiserId != current.id)) { System.out.println("Event not found."); return null; }
        return e;
    }

    static void viewEventRegistrations(boolean ownOnly) {
        Event e = pickEvent(ownOnly);
        if (e == null) return;
        List<Registration> rs = db.regs.stream().filter(r -> r.eventId == e.id).collect(Collectors.toList());
        if (rs.isEmpty()) { System.out.println("No registrations yet."); return; }
        System.out.println("\nRegistrations for " + e.title + ":");
        for (Registration r : rs) {
            User s = findUser(r.studentId);
            System.out.printf("%s | %-18s | %-10s | Attended: %-3s | Team: %s%n", r.id, s.name, r.status,
                    r.attended ? "Yes" : "No", r.teamName == null ? "-" : r.teamName);
        }
    }

    // ===================== MODULE 3: ATTENDANCE =====================
    static void markAttendance() {
        Event e = pickEvent(true);
        if (e == null) return;
        System.out.println("Enter registration IDs to mark attendance (blank to finish).");
        while (true) {
            String id = readLine("Registration ID: ");
            if (id.isEmpty()) break;
            Registration r = findReg(id);
            if (r == null || r.eventId != e.id) { System.out.println("Invalid registration ID for this event."); continue; }
            if (r.status != Status.CONFIRMED) { System.out.println("Registration is " + r.status + " - cannot check in."); continue; }
            if (r.attended) { System.out.println("Duplicate check-in blocked: already marked present."); continue; }
            r.attended = true;
            save();
            System.out.println("Attendance marked for " + findUser(r.studentId).name);
        }
    }

    static void attendanceReport() {
        Event e = pickEvent(true);
        if (e == null) return;
        printAttendanceReport(e);
    }

    static void printAttendanceReport(Event e) {
        List<Registration> conf = db.regs.stream()
                .filter(r -> r.eventId == e.id && r.status == Status.CONFIRMED).collect(Collectors.toList());
        long present = conf.stream().filter(r -> r.attended).count();
        System.out.println("\n===== ATTENDANCE REPORT: " + e.title + " =====");
        System.out.println("Date: " + e.date + " | Venue: " + e.venue);
        for (Registration r : conf)
            System.out.printf("%s | %-18s | %s%n", r.id, findUser(r.studentId).name, r.attended ? "PRESENT" : "ABSENT");
        System.out.printf("Present: %d / %d (%.1f%%)%n", present, conf.size(),
                conf.isEmpty() ? 0.0 : present * 100.0 / conf.size());
    }

    // ===================== MODULE 4: CERTIFICATES =====================
    static void issueCertificates() {
        Event e = pickEvent(true);
        if (e == null) return;
        if (e.date.isAfter(LocalDate.now())) { System.out.println("Event has not happened yet."); return; }
        int count = 0;
        new File(CERT_DIR).mkdirs();
        for (Registration r : db.regs) {
            if (r.eventId == e.id && r.attended && r.certId == null) {
                r.certId = "CERT-" + e.date.getYear() + "-" + (db.certSeq++);
                writeCertificateFile(r, e);
                count++;
            }
        }
        save();
        System.out.println(count + " certificate(s) issued. Files saved in ./" + CERT_DIR);
    }

    static void writeCertificateFile(Registration r, Event e) {
        User s = findUser(r.studentId);
        String line = "=".repeat(60);
        String text = line + "\n"
                + center("CERTIFICATE OF PARTICIPATION", 60) + "\n" + line + "\n\n"
                + center("This certificate is proudly presented to", 60) + "\n\n"
                + center(s.name.toUpperCase(), 60) + "\n\n"
                + center("for participation in", 60) + "\n"
                + center(e.title, 60) + "\n"
                + center("held on " + e.date + " at " + e.venue, 60) + "\n\n"
                + "Certificate ID: " + r.certId + "\n"
                + "Registration ID: " + r.id + "\n"
                + "Issued on: " + LocalDate.now() + "\n" + line + "\n";
        try { Files.write(Paths.get(CERT_DIR, r.certId + ".txt"), text.getBytes()); }
        catch (IOException ex) { System.out.println("Could not write certificate file: " + ex.getMessage()); }
    }

    static String center(String s, int w) {
        if (s.length() >= w) return s;
        return " ".repeat((w - s.length()) / 2) + s;
    }

    static void verifyCertificate() {
        String id = readLine("Enter certificate ID: ");
        for (Registration r : db.regs) {
            if (r.certId != null && r.certId.equalsIgnoreCase(id)) {
                Event e = findEvent(r.eventId);
                System.out.println("VALID certificate.");
                System.out.println("Student: " + findUser(r.studentId).name);
                System.out.println("Event:   " + e.title + " (" + e.date + ")");
                return;
            }
        }
        System.out.println("INVALID - no certificate found with that ID.");
    }

    // ===================== STUDENT =====================
    static void studentMenu() {
        while (true) {
            System.out.println("\n--- STUDENT MENU ---");
            System.out.println("1. Browse events");
            System.out.println("2. Register for an event");
            System.out.println("3. My registrations / status");
            System.out.println("4. Cancel registration");
            System.out.println("5. My certificates");
            System.out.println("6. My participation profile");
            System.out.println("0. Logout");
            switch (readInt("Choice: ")) {
                case 1: browseEvents(); break;
                case 2: registerForEvent(); break;
                case 3: myRegistrations(); break;
                case 4: cancelRegistration(); break;
                case 5: myCertificates(); break;
                case 6: profile(); break;
                case 0: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void browseEvents() {
        String cat = readLine("Filter by category (blank = all): ");
        List<Event> list = db.events.stream()
                .filter(e -> !e.cancelled && !e.date.isBefore(LocalDate.now()))
                .filter(e -> cat.isEmpty() || e.category.equalsIgnoreCase(cat))
                .collect(Collectors.toList());
        System.out.println("\nUpcoming events:");
        listEvents(list, true);
    }

    // ===================== MODULE 2: REGISTRATION =====================
    static void registerForEvent() {
        browseEvents();
        int id = readInt("Event ID to register (0 to go back): ");
        if (id == 0) return;
        Event e = findEvent(id);
        if (e == null || e.cancelled) { System.out.println("Event not available."); return; }
        if (LocalDate.now().isAfter(e.deadline) || e.date.isBefore(LocalDate.now())) {
            System.out.println("Registration closed for this event."); return;
        }
        boolean dup = db.regs.stream().anyMatch(r -> r.eventId == e.id && r.studentId == current.id
                && r.status != Status.CANCELLED);
        if (dup) { System.out.println("You have already registered for this event."); return; }

        Registration r = new Registration();
        r.id = "REG" + (db.regSeq++);
        r.eventId = e.id;
        r.studentId = current.id;
        String team = readLine("Team name (blank = individual): ");
        r.teamName = team.isEmpty() ? null : team;
        r.status = confirmedCount(e.id) < e.maxParticipants ? Status.CONFIRMED : Status.WAITLISTED;
        db.regs.add(r);
        save();
        System.out.println("Registration ID: " + r.id + " | Status: " + r.status);
        if (r.status == Status.WAITLISTED) System.out.println("Event is full. You are on the waitlist.");
    }

    static List<Registration> myRegs() {
        return db.regs.stream().filter(r -> r.studentId == current.id).collect(Collectors.toList());
    }

    static void myRegistrations() {
        List<Registration> rs = myRegs();
        if (rs.isEmpty()) { System.out.println("You have no registrations."); return; }
        for (Registration r : rs) {
            Event e = findEvent(r.eventId);
            System.out.printf("%s | %-25s | %s | %-10s | %s%n", r.id, e.title, e.date, r.status,
                    r.attended ? "Attended" : "-");
        }
    }

    static void cancelRegistration() {
        myRegistrations();
        String id = readLine("Registration ID to cancel (blank = back): ");
        if (id.isEmpty()) return;
        Registration r = findReg(id);
        if (r == null || r.studentId != current.id || r.status == Status.CANCELLED) {
            System.out.println("Registration not found."); return;
        }
        if (r.attended) { System.out.println("Cannot cancel after attending."); return; }
        r.status = Status.CANCELLED;
        promoteWaitlist(findEvent(r.eventId));
        save();
        System.out.println("Registration cancelled.");
    }

    /** Moves waitlisted students to confirmed whenever seats are free (first come, first served). */
    static void promoteWaitlist(Event e) {
        for (Registration r : db.regs) {
            if (confirmedCount(e.id) >= e.maxParticipants) break;
            if (r.eventId == e.id && r.status == Status.WAITLISTED) {
                r.status = Status.CONFIRMED;
                System.out.println("(Waitlisted registration " + r.id + " is now CONFIRMED)");
            }
        }
    }

    static void myCertificates() {
        boolean any = false;
        for (Registration r : myRegs()) {
            if (r.certId != null) {
                any = true;
                System.out.println(r.certId + " - " + findEvent(r.eventId).title
                        + "  (file: " + CERT_DIR + "/" + r.certId + ".txt)");
            }
        }
        if (!any) System.out.println("No certificates yet.");
    }

    // ===================== MODULE 5: PARTICIPATION PROFILE =====================
    static void profile() {
        List<Registration> rs = myRegs().stream()
                .filter(r -> r.status != Status.CANCELLED).collect(Collectors.toList());
        long attended = rs.stream().filter(r -> r.attended).count();
        long certs = rs.stream().filter(r -> r.certId != null).count();
        long comps = rs.stream().filter(r -> {
            String c = findEvent(r.eventId).category.toLowerCase();
            return r.attended && (c.contains("competition") || c.contains("hackathon")
                    || c.contains("coding") || c.contains("debate") || c.contains("dance"));
        }).count();
        System.out.println("\n===== PARTICIPATION PROFILE: " + current.name + " =====");
        System.out.println("Total events registered : " + rs.size());
        System.out.println("Events attended         : " + attended);
        System.out.println("Competitions participated: " + comps);
        System.out.println("Certificates earned     : " + certs);
        System.out.printf("Attendance rate         : %.1f%%%n", rs.isEmpty() ? 0.0 : attended * 100.0 / rs.size());
        System.out.println("\nParticipation history:");
        for (Registration r : rs) {
            Event e = findEvent(r.eventId);
            System.out.printf(" - %s (%s, %s) -> %s%n", e.title, e.category, e.date,
                    r.attended ? "Attended" : r.status.toString());
        }
    }

    // ===================== MODULE 6: ANALYTICS =====================
    static void analytics() {
        System.out.println("\n===== OVERALL ANALYTICS =====");
        long students = db.users.stream().filter(u -> u.role == Role.STUDENT).count();
        long organisers = db.users.stream().filter(u -> u.role == Role.ORGANISER && u.approved).count();
        long active = db.regs.stream().filter(r -> r.status != Status.CANCELLED).count();
        long attended = db.regs.stream().filter(r -> r.attended).count();
        System.out.println("Students: " + students + " | Approved organisers: " + organisers);
        System.out.println("Events: " + db.events.size() + " | Active registrations: " + active
                + " | Attended: " + attended);

        System.out.println("\nPer-event statistics:");
        System.out.printf("%-4s %-24s %-10s %-6s %-5s %-8s %-6s%n", "ID", "Title", "State", "Conf.", "Wait", "Present", "Att%");
        for (Event e : db.events) {
            long conf = confirmedCount(e.id);
            long pres = db.regs.stream().filter(r -> r.eventId == e.id && r.attended).count();
            System.out.printf("%-4d %-24s %-10s %-6d %-5d %-8d %-6.1f%n", e.id,
                    e.title.length() > 23 ? e.title.substring(0, 23) : e.title, eventState(e),
                    conf, waitlistCount(e.id), pres, conf == 0 ? 0.0 : pres * 100.0 / conf);
        }

        System.out.println("\nRegistrations by category:");
        Map<String, Long> byCat = db.events.stream().collect(Collectors.groupingBy(
                e -> e.category, TreeMap::new,
                Collectors.summingLong(e -> db.regs.stream()
                        .filter(r -> r.eventId == e.id && r.status != Status.CANCELLED).count())));
        byCat.forEach((k, v) -> System.out.println(" " + k + ": " + v));
    }
}
