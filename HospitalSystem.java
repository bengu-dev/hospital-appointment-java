import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

/**
 * 🏥 Hastane Randevu Sistemi
 * Kavramlar: OOP, Enum, Stream API, File I/O, Comparable, LocalDate
 */

enum Specialty { CARDIOLOGY, NEUROLOGY, ORTHOPEDICS, DERMATOLOGY, GENERAL }
enum AppointmentStatus { PENDING, CONFIRMED, CANCELLED, COMPLETED }

class Doctor {
    private String id;
    private String name;
    private Specialty specialty;
    private List<LocalDateTime> availableSlots;
    private double consultationFee;

    public Doctor(String id, String name, Specialty specialty, double fee) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.consultationFee = fee;
        this.availableSlots = new ArrayList<>();
    }

    public void addAvailableSlot(LocalDateTime slot) { availableSlots.add(slot); }

    public boolean isAvailable(LocalDateTime slot) { return availableSlots.contains(slot); }

    public void removeSlot(LocalDateTime slot) { availableSlots.remove(slot); }

    public void printAvailableSlots() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("Dr. " + name + " (" + specialty + ") müsait saatler:");
        availableSlots.stream()
            .sorted()
            .forEach(s -> System.out.println("  → " + s.format(fmt)));
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public Specialty getSpecialty() { return specialty; }
    public double getFee()          { return consultationFee; }
}

class Patient {
    private String id;
    private String name;
    private int age;
    private String phone;
    private String bloodType;
    private List<String> medicalHistory;

    public Patient(String id, String name, int age, String phone, String bloodType) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.bloodType = bloodType;
        this.medicalHistory = new ArrayList<>();
    }

    public void addMedicalNote(String note) { medicalHistory.add(note); }

    public void printProfile() {
        System.out.println("\n--- HASTA PROFİLİ ---");
        System.out.println("Ad    : " + name);
        System.out.println("Yaş   : " + age);
        System.out.println("Kan   : " + bloodType);
        System.out.println("Tel   : " + phone);
        if (!medicalHistory.isEmpty()) {
            System.out.println("Geçmiş:");
            medicalHistory.forEach(n -> System.out.println("  • " + n));
        }
    }

    public String getId()   { return id; }
    public String getName() { return name; }
}

class Appointment implements Comparable<Appointment> {
    private String id;
    private Doctor doctor;
    private Patient patient;
    private LocalDateTime dateTime;
    private AppointmentStatus status;
    private String notes;

    public Appointment(Doctor doctor, Patient patient, LocalDateTime dateTime) {
        this.id = "APT-" + System.currentTimeMillis();
        this.doctor = doctor;
        this.patient = patient;
        this.dateTime = dateTime;
        this.status = AppointmentStatus.PENDING;
    }

    public void confirm()  { this.status = AppointmentStatus.CONFIRMED; }
    public void cancel()   { this.status = AppointmentStatus.CANCELLED; }
    public void complete(String notes) {
        this.status = AppointmentStatus.COMPLETED;
        this.notes = notes;
        patient.addMedicalNote("[" + dateTime.toLocalDate() + "] Dr." + doctor.getName() + ": " + notes);
    }

    @Override
    public int compareTo(Appointment other) { return this.dateTime.compareTo(other.dateTime); }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format("[%s] %s | Dr.%-15s | Hasta: %-15s | %s | %.2f TL",
            id, dateTime.format(fmt), doctor.getName(),
            patient.getName(), status, doctor.getFee());
    }

    public String getId()              { return id; }
    public Doctor getDoctor()          { return doctor; }
    public Patient getPatient()        { return patient; }
    public LocalDateTime getDateTime() { return dateTime; }
    public AppointmentStatus getStatus() { return status; }
}

class Hospital {
    private String name;
    private Map<String, Doctor> doctors = new LinkedHashMap<>();
    private Map<String, Patient> patients = new LinkedHashMap<>();
    private List<Appointment> appointments = new ArrayList<>();

    public Hospital(String name) { this.name = name; }

    public void addDoctor(Doctor d)   { doctors.put(d.getId(), d); }
    public void addPatient(Patient p) { patients.put(p.getId(), p); }

    public Appointment bookAppointment(String patientId, String doctorId, LocalDateTime slot) {
        Doctor doctor = doctors.get(doctorId);
        Patient patient = patients.get(patientId);

        if (doctor == null)  throw new IllegalArgumentException("❌ Doktor bulunamadı!");
        if (patient == null) throw new IllegalArgumentException("❌ Hasta bulunamadı!");
        if (!doctor.isAvailable(slot)) throw new IllegalStateException("❌ Bu saat dolu!");

        Appointment apt = new Appointment(doctor, patient, slot);
        apt.confirm();
        doctor.removeSlot(slot);
        appointments.add(apt);

        System.out.printf("✅ Randevu oluşturuldu! [%s] %s → Dr.%s%n",
            apt.getId(), patient.getName(), doctor.getName());
        saveAppointmentToFile(apt);
        return apt;
    }

    public void cancelAppointment(String appointmentId) {
        appointments.stream()
            .filter(a -> a.getId().equals(appointmentId))
            .findFirst()
            .ifPresentOrElse(a -> {
                a.cancel();
                a.getDoctor().addAvailableSlot(a.getDateTime());
                System.out.println("✅ Randevu iptal edildi: " + appointmentId);
            }, () -> System.out.println("❌ Randevu bulunamadı!"));
    }

    public void listBySpecialty(Specialty specialty) {
        System.out.println("\n--- " + specialty + " Uzmanları ---");
        doctors.values().stream()
            .filter(d -> d.getSpecialty() == specialty)
            .forEach(d -> { d.printAvailableSlots(); System.out.printf("  Ücret: %.2f TL%n", d.getFee()); });
    }

    public void printDailySchedule(LocalDate date) {
        System.out.println("\n===== " + date + " GÜNLÜK PROGRAM =====");
        appointments.stream()
            .filter(a -> a.getDateTime().toLocalDate().equals(date))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .sorted()
            .forEach(System.out::println);
    }

    public void printStatistics() {
        System.out.println("\n===== HASTANE İSTATİSTİKLERİ =====");
        System.out.println("Toplam Doktor  : " + doctors.size());
        System.out.println("Toplam Hasta   : " + patients.size());
        System.out.println("Toplam Randevu : " + appointments.size());
        Map<AppointmentStatus, Long> stats = appointments.stream()
            .collect(Collectors.groupingBy(Appointment::getStatus, Collectors.counting()));
        stats.forEach((s, c) -> System.out.println("  " + s + ": " + c));
        double revenue = appointments.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
            .mapToDouble(a -> a.getDoctor().getFee()).sum();
        System.out.printf("Toplam Gelir   : %.2f TL%n", revenue);
    }

    private void saveAppointmentToFile(Appointment apt) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("appointments.txt", true))) {
            w.write(apt.toString());
            w.newLine();
        } catch (IOException e) { System.out.println("Kayıt hatası: " + e.getMessage()); }
    }
}

public class HospitalSystem {
    public static void main(String[] args) {
        Hospital hospital = new Hospital("AGÜ Hastanesi");

        // Doktorlar
        Doctor d1 = new Doctor("D001", "Ayşe Demir", Specialty.CARDIOLOGY, 350.0);
        Doctor d2 = new Doctor("D002", "Mehmet Kaya", Specialty.NEUROLOGY, 400.0);
        Doctor d3 = new Doctor("D003", "Fatma Yıldız", Specialty.GENERAL, 200.0);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        d1.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(9, 0)));
        d1.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(10, 0)));
        d1.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(11, 0)));
        d2.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(14, 0)));
        d2.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(15, 0)));
        d3.addAvailableSlot(LocalDateTime.of(tomorrow, LocalTime.of(9, 30)));

        // Hastalar
        Patient p1 = new Patient("P001", "Bengü Gedik", 21, "0555-111-2233", "A+");
        Patient p2 = new Patient("P002", "Ahmet Yılmaz", 35, "0544-222-3344", "B-");
        Patient p3 = new Patient("P003", "Zeynep Çelik", 28, "0533-333-4455", "O+");

        hospital.addDoctor(d1); hospital.addDoctor(d2); hospital.addDoctor(d3);
        hospital.addPatient(p1); hospital.addPatient(p2); hospital.addPatient(p3);

        hospital.listBySpecialty(Specialty.CARDIOLOGY);

        Appointment apt1 = hospital.bookAppointment("P001", "D001",
            LocalDateTime.of(tomorrow, LocalTime.of(9, 0)));
        Appointment apt2 = hospital.bookAppointment("P002", "D002",
            LocalDateTime.of(tomorrow, LocalTime.of(14, 0)));
        Appointment apt3 = hospital.bookAppointment("P003", "D003",
            LocalDateTime.of(tomorrow, LocalTime.of(9, 30)));

        apt1.complete("Tansiyon normal, 6 ay sonra kontrol.");
        apt3.complete("Hafif grip, antibiyotik yazıldı.");
        hospital.cancelAppointment(apt2.getId());

        p1.printProfile();
        hospital.printDailySchedule(tomorrow);
        hospital.printStatistics();
    }
}
