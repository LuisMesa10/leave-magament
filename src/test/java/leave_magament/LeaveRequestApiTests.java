package leave_magament;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private long create() throws Exception {
        var response = mvc.perform(post("/api/leaves").contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeName\":\"  Ana  \",\"type\":\"VACATION\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeName").value("Ana"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.requestDate").isNotEmpty()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void rejectsBlankFieldsAndClientAssignedIdentity() throws Exception {
        mvc.perform(post("/api/leaves").contentType(MediaType.APPLICATION_JSON)
            .content("{\"employeeName\":\" \",\"type\":\"VACATION\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/leaves").contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1,\"employeeName\":\"Ana\",\"type\":\"VACATION\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void approvalCannotBeReversed() throws Exception {
        long id = create();
        mvc.perform(put("/api/leaves/" + id + "/approve"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(put("/api/leaves/" + id + "/reject")).andExpect(status().isConflict());
    }
    @Test void rejectionIsFinalAndDeletionChecksExistence() throws Exception {
        long id = create();
        mvc.perform(put("/api/leaves/" + id + "/reject"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(put("/api/leaves/" + id + "/approve")).andExpect(status().isConflict());
        mvc.perform(delete("/api/leaves/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/leaves/" + id)).andExpect(status().isNotFound());
    }
    @Test void missingRequestReturns404() throws Exception {
        mvc.perform(put("/api/leaves/999999/approve")).andExpect(status().isNotFound());
        mvc.perform(put("/api/leaves/999999/reject")).andExpect(status().isNotFound());
    }
    @Test void exportDownloadsActualData() throws Exception {
        create();
        mvc.perform(get("/api/leaves/export")).andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"leave_requests.txt\""))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Empleado: Ana")));
    }
}
