package leave_magament;

import leave_magament.model.*;
import leave_magament.util.FileExporter;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FileExporterTests {
    @Test void csvEscapesQuotesCommasLineBreaksAndFormulas() {
        var request = new LeaveRequest("employee", "Ana, \"A\"", LeaveType.OTHER,
            LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 2),
            "=SUM(A1)\nsegunda linea", LocalDateTime.of(2030, 1, 1, 0, 0));
        var csv = FileExporter.toCsv(List.of(request));
        assertThat(csv).contains("\"Ana, \"\"A\"\"\"")
            .contains("\"'=SUM(A1)\nsegunda linea\"")
            .endsWith("\r\n");
    }
    @Test void emptyExportStillHasHeaders() {
        assertThat(FileExporter.toCsv(List.of())).isEqualTo(
            "id,employeeUsername,employeeName,type,status,startDate,endDate,reason\r\n");
    }
}
