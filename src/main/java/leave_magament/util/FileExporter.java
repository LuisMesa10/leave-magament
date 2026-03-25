package leave_magament.util;

import leave_magament.model.LeaveRequest;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileExporter {
    public static void exportToFile(List<LeaveRequest> requests){
        try(FileWriter writer = new FileWriter("leave_requests.txt")){
                for (LeaveRequest req : requests) {
                    writer.write("ID: " + req.getId() + "\n");
                    writer.write("Empleado: " + req.getEmployeeName() + "\n");
                    writer.write("Tipo: " + req.getType() + "\n");
                    writer.write("Estado: " + req.getStatus() + "\n");
                    writer.write("Fecha: " + req.getRequestDate() + "\n");
                    writer.write("---------------------------\n");
                }
            }

        catch(IOException e){
                e.printStackTrace();
            }

        }
    }

