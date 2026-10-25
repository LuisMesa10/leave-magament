package leave_magament.controller;

import jakarta.validation.Valid;
import leave_magament.dto.CreateLeaveRequest;
import leave_magament.model.LeaveRequest;
import leave_magament.service.LeaveRequestService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {
    private final LeaveRequestService service;
    public LeaveRequestController(LeaveRequestService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<LeaveRequest> create(@Valid @RequestBody CreateLeaveRequest input) {
        LeaveRequest created = service.create(input);
        return ResponseEntity.created(URI.create("/api/leaves/" + created.getId())).body(created);
    }
    @GetMapping
    public List<LeaveRequest> getAll() { return service.getAll(); }
    @PutMapping("/{id}/approve")
    public LeaveRequest approve(@PathVariable Long id) { return service.approve(id); }
    @PutMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable Long id) { return service.reject(id); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leave_requests.txt\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(service.exportRequests().getBytes(StandardCharsets.UTF_8));
    }
}
