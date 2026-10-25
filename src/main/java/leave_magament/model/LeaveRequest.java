package leave_magament.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private Long version;
    @Column(nullable = false, updatable = false, length = 100)
    private String employeeUsername;
    @Column(nullable = false, length = 120) private String employeeName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LeaveType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LeaveStatus status;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(nullable = false, updatable = false) private LocalDateTime requestDate;
    @Column(nullable = false) private LocalDateTime updatedAt;

    protected LeaveRequest() {}
    public LeaveRequest(String username, String name, LeaveType type, LocalDate start,
                        LocalDate end, String reason, LocalDateTime now) {
        this.employeeUsername = username;
        this.status = LeaveStatus.PENDING;
        this.requestDate = now;
        update(name, type, start, end, reason, now);
    }
    public void update(String name, LeaveType type, LocalDate start, LocalDate end,
                       String reason, LocalDateTime now) {
        this.employeeName = name.strip(); this.type = type;
        this.startDate = start; this.endDate = end;
        this.reason = reason.strip(); this.updatedAt = now;
    }
    public void changeStatus(LeaveStatus status, LocalDateTime now) {
        this.status = status; this.updatedAt = now;
    }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getEmployeeUsername() { return employeeUsername; }
    public String getEmployeeName() { return employeeName; }
    public LeaveType getType() { return type; }
    public LeaveStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
