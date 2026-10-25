package leave_magament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// El cliente no puede asignar ID, estado ni fecha de registro.
public record CreateLeaveRequest(
        @NotBlank @Size(max = 120) String employeeName,
        @NotBlank @Size(max = 80) String type) {
}
