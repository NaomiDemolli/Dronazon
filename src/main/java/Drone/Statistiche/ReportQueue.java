package Drone.Statistiche;

import java.util.ArrayList;

public class ReportQueue {
    public ArrayList<Report> buffer = new ArrayList<Report>();

    public synchronized void put(Report report) {
        buffer.add(report);
    }

    public synchronized ArrayList<Report> readAllAndClean() {
        ArrayList<Report> l = new ArrayList<Report>(buffer);
        buffer.clear();
        return l;
    }

    public synchronized ArrayList<Report> getReportsCopy(){
        ArrayList<Report> copy = new ArrayList<>(buffer);
        return copy;
    }

}
