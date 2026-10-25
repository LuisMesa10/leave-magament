package leave_magament.service;

import leave_magament.dto.CreateLeaveRequest;
import leave_magament.model.LeaveRequest;
import leave_magament.repository.leaveRequestRepository;
import leave_magament.util.FileExporter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LeaveRequestService {
    private final leaveRequestRepository repository;
    public LeaveRequestService(leaveRequestRepository repository) {
        this.repository = repository;
    }
    public LeaveRequest create(CreateLeaveRequest input) {
        LeaveRequest request = new LeaveRequest(null, input.employeeName().strip(),
                input.type().strip(), "PENDING", LocalDateTime.now());
        return repository.save(request);
    }
    @Transactional(readOnly = true)
    public List<LeaveRequest> getAll() {
        return repository.findAll();
    }
    public LeaveRequest approve(Long id) { return decide(id, "APPROVED"); }
    public LeaveRequest reject(Long id) { return decide(id, "REJECTED"); }
    private LeaveRequest decide(Long id, String status) {
        LeaveRequest request = find(id);
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden decidir solicitudes pendientes");
        }
        request.setStatus(status);
        return repository.save(request);
    }
    private LeaveRequest find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }
    public void delete(Long id) { repository.delete(find(id)); }
    @Transactional(readOnly = true)
    public String exportRequests() { return FileExporter.toText(repository.findAll()); }
}
