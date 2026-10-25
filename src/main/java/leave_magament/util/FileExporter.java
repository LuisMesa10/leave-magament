package leave_magament.util;

import leave_magament.model.LeaveRequest;
import java.util.List;

// La exportacion se entrega al cliente sin escribir archivos compartidos en el servidor.
public final class FileExporter {
    private FileExporter() {}
    public static String toText(List<LeaveRequest> requests) {
        StringBuilder text = new StringBuilder();
        for (LeaveRequest request : requests) {
            text.append("ID: ").append(request.getId()).append('\n')
                .append("Empleado: ").append(request.getEmployeeName()).append('\n')
                .append("Tipo: ").append(request.getType()).append('\n')
                .append("Estado: ").append(request.getStatus()).append('\n')
                .append("Fecha: ").append(request.getRequestDate()).append('\n')
                .append("---------------------------\n");
        }
        return text.toString();
    }
}
