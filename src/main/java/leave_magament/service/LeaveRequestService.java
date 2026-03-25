package leave_magament.service;

import leave_magament.model.LeaveRequest;
import leave_magament.repository.leaveRequestRepository;
import leave_magament.util.FileExporter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveRequestService {

    private final leaveRequestRepository repository;

    public LeaveRequestService(leaveRequestRepository repository) {
        this.repository = repository;
    }
    public LeaveRequest create(LeaveRequest request){
        request.setStatus("PENDING");
        request.setRequestDate(LocalDateTime.now());

        return repository.save(request);
    }

    public List<LeaveRequest> getAll(){
        return repository.findAll();
    }

    public LeaveRequest approve(Long id){
        LeaveRequest req = repository.findById(id).orElseThrow();
        req.setStatus("APPROVED");
        return repository.save(req);
    }

    public LeaveRequest reject(Long id){
        LeaveRequest req = repository.findById(id).orElseThrow();
        req.setStatus("REJECTED");
        return repository.save(req);
    }

    public void delete(Long id){
        repository.deleteById(id);
    }

    public void exportRequests(){
        List<LeaveRequest> requests = repository.findAll();
        FileExporter.exportToFile(requests);
    }
}
