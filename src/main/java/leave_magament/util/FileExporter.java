package leave_magament.util;

import leave_magament.model.LeaveRequest;
import java.util.List;

public final class FileExporter {
    private FileExporter() {}
    private static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        // Evitar formulas al abrir campos controlados por usuarios en una hoja de calculo.
        String leading = text.stripLeading();
        if (leading.startsWith("=") || leading.startsWith("+") || leading.startsWith("-")
                || leading.startsWith("@") || text.startsWith("\t") || text.startsWith("\r")
                || text.startsWith("\n")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    public static String toCsv(List<LeaveRequest> requests) {
        StringBuilder csv = new StringBuilder("id,employeeUsername,employeeName,type,status,startDate,endDate,reason\r\n");
        for (var r : requests) {
            Object[] fields = {r.getId(), r.getEmployeeUsername(), r.getEmployeeName(), r.getType(),
                    r.getStatus(), r.getStartDate(), r.getEndDate(), r.getReason()};
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) csv.append(',');
                csv.append(cell(fields[i]));
            }
            csv.append("\r\n");
        }
        return csv.toString();
    }
}
