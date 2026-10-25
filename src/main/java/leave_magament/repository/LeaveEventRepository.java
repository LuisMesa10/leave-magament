package leave_magament.repository;
import leave_magament.model.LeaveEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LeaveEventRepository extends JpaRepository<LeaveEvent, Long> {
    Page<LeaveEvent> findByRequestIdOrderByIdAsc(Long requestId, Pageable pageable);
}
