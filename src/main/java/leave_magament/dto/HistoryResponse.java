package leave_magament.dto;
import leave_magament.model.LeaveEvent;
import java.time.LocalDateTime;
public record HistoryResponse(Long id, String action, String actor, String comment,
                              LocalDateTime occurredAt) {
    public static HistoryResponse from(LeaveEvent e) {
        return new HistoryResponse(e.getId(), e.getAction(), e.getActor(), e.getComment(), e.getOccurredAt());
    }
}
