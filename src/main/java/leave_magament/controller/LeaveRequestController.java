package leave_magament.controller;

import leave_magament.model.LeaveRequest;
import leave_magament.service.LeaveRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {
    private final LeaveRequestService service;

    public LeaveRequestController(LeaveRequestService service) {
        this.service = service;
    }

    @PostMapping
    public LeaveRequest create(@RequestBody LeaveRequest request){
        return service.create(request);
    }

    @GetMapping
    public List<LeaveRequest> getAll(){
        return service.getAll();
    }

    @PutMapping("/{id}/approve")
    public LeaveRequest approve(@PathVariable Long id){
        return service.approve(id);
    }

    @PutMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable Long id){
        return service.reject(id);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id){
        service.delete(id);
        return "solicitud eliminada correctamente";
    }

    @GetMapping("/export")
    public String export(){
        service.exportRequests();
        return "ARCHIVO GENERADO CORRECTAMENTE";
    }

}
