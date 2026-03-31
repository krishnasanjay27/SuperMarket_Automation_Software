package service;

import dao.SalesReportDAO;
import model.ProfitByItemDTO;
import model.ProfitBySaleDTO;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SalesReportService {

    private final SalesReportDAO salesReportDAO;

    public SalesReportService() {
        this.salesReportDAO = new SalesReportDAO();
    }

    public List<ProfitBySaleDTO> loadProfitBySaleReport(LocalDate start, LocalDate end, int limit) {
        if (start == null || end == null) {
            System.err.println("loadProfitBySaleReport() failed - start and end dates must not be null.");
            return Collections.emptyList();
        }
        if (end.isBefore(start)) {
            System.err.println("loadProfitBySaleReport() failed - end date is before start date.");
            return Collections.emptyList();
        }
        return salesReportDAO.getProfitBySaleReport(start, end, limit);
    }

    public List<ProfitByItemDTO> loadProfitByItemReport(LocalDate start, LocalDate end, int limit) {
        if (start == null || end == null) {
            System.err.println("loadProfitByItemReport() failed - start and end dates must not be null.");
            return Collections.emptyList();
        }
        if (end.isBefore(start)) {
            System.err.println("loadProfitByItemReport() failed - end date is before start date.");
            return Collections.emptyList();
        }
        return salesReportDAO.getProfitByItemReport(start, end, limit);
    }
}
