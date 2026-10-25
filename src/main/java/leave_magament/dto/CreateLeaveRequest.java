package leave_magament.dto;

import jakarta.validation.constraints.*;
import leave_magament.model.LeaveType;
import java.time.LocalDate;

public record CreateLeaveRequest(
        @NotBlank @Size(max = 120) String employeeName,
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank @Size(max = 1000) String reason) {
    @AssertTrue(message = "La fecha final debe ser igual o posterior a la inicial")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
