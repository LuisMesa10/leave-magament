package leave_magament.service;

import leave_magament.dto.*;
import leave_magament.model.*;
import leave_magament.repository.*;
import leave_magament.util.FileExporter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class LeaveRequestService {
    private final LeaveRequestRepository repository;
    private final LeaveEventRepository events;
    public LeaveRequestService(LeaveRequestRepository repository, LeaveEventRepository events) {
        this.repository = repository; this.events = events;
    }
    private boolean manager(Authentication actor) {
        return actor.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }
    private void owner(LeaveRequest r, Authentication actor) {
        if (!r.getEmployeeUsername().equals(actor.getName()))
            throw new AccessDeniedException("Solo el propietario puede modificar la solicitud");
    }
    private LeaveRequest visible(Long id, Authentication actor) {
        // Filtrar antes de responder para no revelar solicitudes ajenas.
        return repository.findOne((root, query, cb) -> {
            var byId = cb.equal(root.get("id"), id);
            return manager(actor) ? byId : cb.and(byId,
                    cb.equal(root.get("employeeUsername"), actor.getName()));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }
    private void pending(LeaveRequest r) {
        if (r.getStatus() != LeaveStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya tiene una decision final");
    }
    private void future(CreateLeaveRequest input) {
        if (input.startDate().isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede estar en el pasado");
    }
    private String snapshot(LeaveRequest r) {
        return r.getEmployeeName() + " | " + r.getType() + " | " + r.getStartDate()
                + " / " + r.getEndDate() + " | " + r.getReason();
    }
    public LeaveResponse create(CreateLeaveRequest input, Authentication actor) {
        future(input);
        var r = repository.saveAndFlush(new LeaveRequest(actor.getName(), input.employeeName(), input.type(),
                input.startDate(), input.endDate(), input.reason(), LocalDateTime.now()));
        events.save(new LeaveEvent(r, "CREATED", actor.getName(), snapshot(r)));
        return LeaveResponse.from(r);
    }
    @Transactional(readOnly = true)
    public LeaveResponse get(Long id, Authentication actor) { return LeaveResponse.from(visible(id, actor)); }

    public LeaveResponse update(Long id, UpdateLeaveRequest input, Authentication actor) {
        var r = visible(id, actor); owner(r, actor); pending(r); future(input.details());
        if (!Objects.equals(r.getVersion(), input.version()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version desactualizada; vuelva a consultar");
        var d = input.details();
        r.update(d.employeeName(), d.type(), d.startDate(), d.endDate(), d.reason(), LocalDateTime.now());
        repository.saveAndFlush(r);
        events.save(new LeaveEvent(r, "UPDATED", actor.getName(), snapshot(r)));
        return LeaveResponse.from(r);
    }
    public LeaveResponse decide(Long id, LeaveStatus target, DecisionRequest input, Authentication actor) {
        if (!manager(actor)) throw new AccessDeniedException("Se requiere rol de supervisor");
        if (target != LeaveStatus.APPROVED && target != LeaveStatus.REJECTED)
            throw new IllegalArgumentException("Decision invalida");
        var r = visible(id, actor); pending(r);
        r.changeStatus(target, LocalDateTime.now()); repository.saveAndFlush(r);
        events.save(new LeaveEvent(r, target.name(), actor.getName(), input.comment().strip()));
        return LeaveResponse.from(r);
    }
    public LeaveResponse cancel(Long id, DecisionRequest input, Authentication actor) {
        var r = visible(id, actor); owner(r, actor); pending(r);
        r.changeStatus(LeaveStatus.CANCELLED, LocalDateTime.now()); repository.saveAndFlush(r);
        events.save(new LeaveEvent(r, "CANCELLED", actor.getName(), input.comment().strip()));
        return LeaveResponse.from(r);
    }
    private Pageable page(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0; size entre 1 y 100");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
    private Specification<LeaveRequest> filter(Authentication actor, LeaveStatus status, LeaveType type,
            String employee, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de consulta invalido");
        if (employee != null && employee.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employee admite hasta 100 caracteres");
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!manager(actor)) predicates.add(cb.equal(root.get("employeeUsername"), actor.getName()));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (employee != null && !employee.isBlank())
                predicates.add(cb.equal(root.get("employeeUsername"), employee.strip()));
            // Interseccion inclusiva del periodo solicitado con [from,to].
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
    @Transactional(readOnly = true)
    public PageResponse<LeaveResponse> search(Authentication actor, LeaveStatus status, LeaveType type,
            String employee, LocalDate from, LocalDate to, int page, int size) {
        return PageResponse.from(repository.findAll(filter(actor, status, type, employee, from, to),
                page(page, size)).map(LeaveResponse::from));
    }
    @Transactional(readOnly = true)
    public PageResponse<HistoryResponse> history(Long id, Authentication actor, int page, int size) {
        visible(id, actor);
        Pageable paging = page(page, size);
        return PageResponse.from(events.findByRequestIdOrderByIdAsc(id,
                PageRequest.of(paging.getPageNumber(), paging.getPageSize())).map(HistoryResponse::from));
    }
    @Transactional(readOnly = true)
    public Map<LeaveStatus, Long> summary(Authentication actor) {
        Map<LeaveStatus, Long> result = new EnumMap<>(LeaveStatus.class);
        for (var status : LeaveStatus.values())
            result.put(status, repository.count(filter(actor, status, null, null, null, null)));
        return result;
    }
    @Transactional(readOnly = true)
    public String export(Authentication actor, LeaveStatus status, LeaveType type,
                         String employee, LocalDate from, LocalDate to) {
        var matches = repository.findAll(filter(actor, status, type, employee, from, to),
                PageRequest.of(0, 1000, Sort.by("id")));
        if (matches.getTotalElements() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La exportacion admite 1000 registros; reduzca el rango con filtros");
        return FileExporter.toCsv(matches.getContent());
    }
}
