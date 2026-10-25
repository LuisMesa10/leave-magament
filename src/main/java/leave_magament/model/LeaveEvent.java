package leave_magament.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Eventos append-only en la API; no se ofrecen endpoints de edicion ni borrado.
@Entity
public class LeaveEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false) private LeaveRequest request;
    @Column(nullable = false, length = 20) private String action;
    @Column(nullable = false, length = 100) private String actor;
    @Column(nullable = false, length = 2000) private String comment;
    @Column(nullable = false) private LocalDateTime occurredAt;
    protected LeaveEvent() {}
    public LeaveEvent(LeaveRequest request, String action, String actor, String comment) {
        this.request = request; this.action = action; this.actor = actor;
        this.comment = comment; this.occurredAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getComment() { return comment; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
