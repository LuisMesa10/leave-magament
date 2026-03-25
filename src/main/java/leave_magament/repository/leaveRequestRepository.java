package leave_magament.repository;

import leave_magament.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;


public interface leaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
}
