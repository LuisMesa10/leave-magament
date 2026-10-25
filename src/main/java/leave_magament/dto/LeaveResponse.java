package leave_magament.dto;

import leave_magament.model.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

public record LeaveResponse(Long id, Long version, String employeeUsername, String employeeName,
        LeaveType type, LeaveStatus status, LocalDate startDate, LocalDate endDate,
        long calendarDays, String reason, LocalDateTime requestDate, LocalDateTime updatedAt) {
    public static LeaveResponse from(LeaveRequest r) {
        return new LeaveResponse(r.getId(), r.getVersion(), r.getEmployeeUsername(),
            r.getEmployeeName(), r.getType(), r.getStatus(), r.getStartDate(), r.getEndDate(),
            ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) + 1,
            r.getReason(), r.getRequestDate(), r.getUpdatedAt());
    }
}
