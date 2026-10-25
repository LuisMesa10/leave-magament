package leave_magament.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record DecisionRequest(@NotBlank @Size(max = 1000) String comment) {}
