package leave_magament;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    private String details(String name) {
        return "{\"employeeName\":\"" + name + "\",\"type\":\"VACATION\",\"startDate\":\""
            + LocalDate.now().plusDays(10) + "\",\"endDate\":\"" + LocalDate.now().plusDays(12)
            + "\",\"reason\":\"Descanso\"}";
    }
    private long create() throws Exception {
        var response = mvc.perform(post("/api/leaves").with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON).content(details("Ana")))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.employeeUsername").value("employee"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.calendarDays").value(3)).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void basicAuthenticationAndHeaderAreRequired() throws Exception {
        mvc.perform(get("/api/leaves")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leaves").with(httpBasic("employee", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leaves").with(httpBasic("employee", "test-employee-password")))
            .andExpect(status().isOk());
        mvc.perform(post("/api/leaves").with(httpBasic("employee", "test-employee-password"))
            .contentType(MediaType.APPLICATION_JSON).content(details("Ana"))).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void validationRejectsBadDatesTypesAndIdentity() throws Exception {
        String valid = details("Ana");
        String[] invalid = {details(" "), valid.replace("VACATION", "UNKNOWN"),
            valid.replace("\"reason\":\"Descanso\"", "\"reason\":\" \""),
            valid.replace("{", "{\"id\":1,"),
            valid.replace(LocalDate.now().plusDays(12).toString(), LocalDate.now().plusDays(9).toString()),
            valid.replace(LocalDate.now().plusDays(10).toString(), LocalDate.now().minusDays(1).toString())};
        for (String body : invalid)
            mvc.perform(post("/api/leaves").header("X-Requested-With", "leave-api")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    @Test void ownerCanEditPendingRequestButStaleVersionConflicts() throws Exception {
        long id = create();
        String update = "{\"version\":0,\"details\":" + details("Ana Perez") + "}";
        mvc.perform(put("/api/leaves/" + id).with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.employeeName").value("Ana Perez"));
        mvc.perform(put("/api/leaves/" + id).with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/leaves/" + id + "/history").with(user("employee").roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].action").value("CREATED"))
            .andExpect(jsonPath("$.content[1].action").value("UPDATED"));
    }
    @Test void managerDecidesAndFinalDecisionCannotBeReversed() throws Exception {
        long id = create();
        String path = "/api/leaves/" + id;
        mvc.perform(post(path + "/approve").with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"OK\"}")).andExpect(status().isForbidden());
        mvc.perform(post(path + "/reject").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(post(path + "/approve").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Cobertura verificada\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post(path + "/reject").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Cambio\"}")).andExpect(status().isConflict());
        mvc.perform(post(path + "/cancel").with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Cambio\"}")).andExpect(status().isConflict());
        mvc.perform(put(path).with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":1,\"details\":" + details("Ana") + "}")).andExpect(status().isConflict());
    }
    @Test void pendingRequestCanBeCancelledAndHistoryRemains() throws Exception {
        long id = create();
        mvc.perform(post("/api/leaves/" + id + "/cancel").with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Ya no necesito el permiso\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/leaves/" + id + "/history").with(user("manager").roles("MANAGER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[1].action").value("CANCELLED"));
    }
    @Test void foreignRequestsAreHiddenAcrossAllReadPaths() throws Exception {
        long id = create();
        var other = user("other").roles("EMPLOYEE");
        mvc.perform(get("/api/leaves/" + id).with(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/leaves/" + id + "/history").with(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/leaves").with(other)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/leaves/summary").with(other)).andExpect(status().isOk())
            .andExpect(jsonPath("$.PENDING").value(0));
        mvc.perform(get("/api/leaves/export").with(other)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Ana"))));
        mvc.perform(put("/api/leaves/" + id).with(other).header("X-Requested-With", "leave-api")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":0,\"details\":" + details("Otro") + "}")).andExpect(status().isNotFound());
    }
    @Test void managerCannotCreateOrEditRequests() throws Exception {
        long id = create();
        mvc.perform(post("/api/leaves").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content(details("Manager"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/leaves/" + id + "/cancel").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Cancel\"}")).andExpect(status().isForbidden());
    }
    @Test void filtersPaginationSummaryAndCsvAreAvailable() throws Exception {
        create();
        var manager = user("manager").roles("MANAGER");
        mvc.perform(get("/api/leaves").with(manager).param("size", "1")
            .param("status", "PENDING").param("type", "VACATION")
            .param("from", LocalDate.now().plusDays(11).toString()).param("employee", "employee"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.size").value(1)).andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/leaves").with(manager).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaves").with(manager).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaves").with(manager).param("from", "2030-02-02")
            .param("to", "2030-01-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaves/summary").with(manager)).andExpect(status().isOk())
            .andExpect(jsonPath("$.PENDING", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/leaves/export").with(manager).param("status", "PENDING"))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition",
                "attachment; filename=\"leave_requests.csv\""))
            .andExpect(content().string(containsString("employeeUsername")));
        mvc.perform(get("/api/leaves/999999").with(manager)).andExpect(status().isNotFound());
    }
    @Test void rejectionIsRecordedAndCsvEscapesUntrustedText() throws Exception {
        var response = mvc.perform(post("/api/leaves").with(user("employee").roles("EMPLOYEE"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content(details("=1+1"))).andExpect(status().isCreated()).andReturn();
        long id = mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/leaves/" + id + "/reject").with(user("manager").roles("MANAGER"))
            .header("X-Requested-With", "leave-api").contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Falta cobertura\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(get("/api/leaves/export").with(user("employee").roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("\"'=1+1\"")));
    }
}
