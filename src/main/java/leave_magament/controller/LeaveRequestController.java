package leave_magament.controller;

import jakarta.validation.Valid;
import leave_magament.dto.*;
import leave_magament.model.*;
import leave_magament.service.LeaveRequestService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {
    private final LeaveRequestService service;
    public LeaveRequestController(LeaveRequestService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<LeaveResponse> create(@Valid @RequestBody CreateLeaveRequest input, Authentication actor) {
        var created = service.create(input, actor);
        return ResponseEntity.created(URI.create("/api/leaves/" + created.id())).body(created);
    }
    @GetMapping("/{id}")
    public LeaveResponse get(@PathVariable Long id, Authentication actor) { return service.get(id, actor); }
    @PutMapping("/{id}")
    public LeaveResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLeaveRequest input,
                                Authentication actor) { return service.update(id, input, actor); }
    @PostMapping("/{id}/approve")
    public LeaveResponse approve(@PathVariable Long id, @Valid @RequestBody DecisionRequest input,
                                 Authentication actor) { return service.decide(id, LeaveStatus.APPROVED, input, actor); }
    @PostMapping("/{id}/reject")
    public LeaveResponse reject(@PathVariable Long id, @Valid @RequestBody DecisionRequest input,
                                Authentication actor) { return service.decide(id, LeaveStatus.REJECTED, input, actor); }
    @PostMapping("/{id}/cancel")
    public LeaveResponse cancel(@PathVariable Long id, @Valid @RequestBody DecisionRequest input,
                                Authentication actor) { return service.cancel(id, input, actor); }
    @GetMapping
    public PageResponse<LeaveResponse> search(Authentication actor,
            @RequestParam(required = false) LeaveStatus status, @RequestParam(required = false) LeaveType type,
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.search(actor, status, type, employee, from, to, page, size);
    }
    @GetMapping("/{id}/history")
    public PageResponse<HistoryResponse> history(@PathVariable Long id, Authentication actor,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.history(id, actor, page, size);
    }
    @GetMapping("/summary")
    public Map<LeaveStatus, Long> summary(Authentication actor) { return service.summary(actor); }
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(Authentication actor,
            @RequestParam(required = false) LeaveStatus status, @RequestParam(required = false) LeaveType type,
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leave_requests.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(service.export(actor, status, type, employee, from, to).getBytes(StandardCharsets.UTF_8));
    }
}
