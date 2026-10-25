package leave_magament.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record UpdateLeaveRequest(@NotNull @PositiveOrZero Long version,
                                 @NotNull @Valid CreateLeaveRequest details) {}
