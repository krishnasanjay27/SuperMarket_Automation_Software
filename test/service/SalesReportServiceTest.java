package service;

import dao.SalesReportDAO;
import model.ProfitByItemDTO;
import model.ProfitBySaleDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesReportService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalesReportService Tests")
class SalesReportServiceTest {

    @Mock
    private SalesReportDAO salesReportDAO;

    private SalesReportService salesReportService;

    @BeforeEach
    void setUp() {
        salesReportService = new SalesReportService(salesReportDAO);
    }

    // ─── loadProfitBySaleReport() ─────────────────────────────────

    @Nested
    @DisplayName("loadProfitBySaleReport()")
    class ProfitBySaleTests {

        @Test
        @DisplayName("Returns empty list when start date is null")
        void profitBySale_nullStart_returnsEmpty() {
            List<ProfitBySaleDTO> result = salesReportService.loadProfitBySaleReport(
                    null, LocalDate.now(), 10);
            assertTrue(result.isEmpty());
            verifyNoInteractions(salesReportDAO);
        }

        @Test
        @DisplayName("Returns empty list when end date is null")
        void profitBySale_nullEnd_returnsEmpty() {
            List<ProfitBySaleDTO> result = salesReportService.loadProfitBySaleReport(
                    LocalDate.now(), null, 10);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when end is before start")
        void profitBySale_endBeforeStart_returnsEmpty() {
            LocalDate start = LocalDate.of(2026, 3, 31);
            LocalDate end   = LocalDate.of(2026, 3, 1);

            List<ProfitBySaleDTO> result = salesReportService.loadProfitBySaleReport(
                    start, end, 10);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns report data on valid range")
        void profitBySale_validRange_returnsData() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end   = LocalDate.of(2026, 3, 31);
            List<ProfitBySaleDTO> data = Arrays.asList(
                    new ProfitBySaleDTO("TXN001", 500.0, 350.0, 150.0),
                    new ProfitBySaleDTO("TXN002", 300.0, 200.0, 100.0));

            when(salesReportDAO.getProfitBySaleReport(start, end, 10)).thenReturn(data);

            List<ProfitBySaleDTO> result = salesReportService.loadProfitBySaleReport(
                    start, end, 10);

            assertEquals(2, result.size());
            assertEquals("TXN001", result.get(0).getTransactionId());
        }
    }

    // ─── loadProfitByItemReport() ─────────────────────────────────

    @Nested
    @DisplayName("loadProfitByItemReport()")
    class ProfitByItemTests {

        @Test
        @DisplayName("Returns empty list when start date is null")
        void profitByItem_nullStart_returnsEmpty() {
            assertTrue(salesReportService.loadProfitByItemReport(
                    null, LocalDate.now(), 10).isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when end date is null")
        void profitByItem_nullEnd_returnsEmpty() {
            assertTrue(salesReportService.loadProfitByItemReport(
                    LocalDate.now(), null, 10).isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when end is before start")
        void profitByItem_endBeforeStart_returnsEmpty() {
            assertTrue(salesReportService.loadProfitByItemReport(
                    LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), 10).isEmpty());
        }

        @Test
        @DisplayName("Returns report data on valid range")
        void profitByItem_validRange_returnsData() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end   = LocalDate.of(2026, 3, 31);
            List<ProfitByItemDTO> data = List.of(
                    new ProfitByItemDTO("ITM001", "Rice", 50, 3999.50));

            when(salesReportDAO.getProfitByItemReport(start, end, 10)).thenReturn(data);

            List<ProfitByItemDTO> result = salesReportService.loadProfitByItemReport(
                    start, end, 10);

            assertEquals(1, result.size());
            assertEquals("ITM001", result.get(0).getItemCode());
            assertEquals(50, result.get(0).getTotalSold());
        }
    }
}
