package leave_magament.repository;
import leave_magament.model.LeaveRequest;
import org.springframework.data.jpa.repository.*;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>,
        JpaSpecificationExecutor<LeaveRequest> {}
