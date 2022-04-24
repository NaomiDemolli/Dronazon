package REST.beans;

import Drone.Utilities.Utils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Reports {

    @XmlElement(name="reports_list")

    private ArrayList<Report> reports;
    private static Reports instance;

    private Reports(){
        reports = new ArrayList<Report>();
    }

    public synchronized static Reports getInstance(){
        if(instance==null)
            instance = new Reports();
        return instance;
    }

    public synchronized ArrayList<Report> getReportList() {
        return new ArrayList<>(reports);
    }

    public synchronized void add(Report report) {
        this.reports.add(report);
    }

    public ArrayList<Report> getFirstN(int n){
        ArrayList<Report> repCopy = getReportList();
        if(n>repCopy.size()){
            Utils.printDetail("Non sono ancora presenti " + n + " report", 1);
            n = repCopy.size();
        }
        return new ArrayList<>(repCopy.subList(0,n));
    }

    public double numDelivery(int t1, int t2){
        ArrayList<Report> reps = getReportList();
        int num = 0;
        int tot = 0;

        for(int i=0; i < reps.size(); i++){
            Report report = reps.get(i);
            if(report.getTimestamp() >= t1 && report.getTimestamp() <= t2){
                num += report.getDelivery();
                tot++;
            }
        }

        return (tot>0)? (double) num/tot: 0;
    }

    public double numKilo(int t1, int t2){
        ArrayList<Report> reps = getReportList();
        int num = 0;
        int tot = 0;

        for(int i=0; i < reps.size(); i++){
            Report report = reps.get(i);
            if(report.getTimestamp() >= t1 && report.getTimestamp() <= t2){
                num += report.getDistance();
                tot++;
            }
        }

        return (tot>0)? num/tot: 0;
    }

    public int size(){
        return reports.size();
    }

    public Report get(int index){
        return reports.get(index);
    }

}
