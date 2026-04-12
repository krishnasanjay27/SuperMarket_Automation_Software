package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Report} model class.
 */
@DisplayName("Report Model Tests")
class ReportTest {

    @Test
    @DisplayName("No-arg constructor creates Report with defaults")
    void noArgConstructor_fieldsAreDefaults() {
        Report r = new Report();

        assertEquals(0, r.getReportId());
        assertNull(r.getGeneratedDate());
        assertNull(r.getReportType());
        assertNull(r.getDateRangeStart());
        assertNull(r.getDateRangeEnd());
        assertNull(r.getGeneratedBy());
    }

    @Test
    @DisplayName("Parameterised constructor sets all fields")
    void parameterizedConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);

        Report r = new Report(1, now, "SALES", start, end, "MGR001");

        assertEquals(1, r.getReportId());
        assertEquals(now, r.getGeneratedDate());
        assertEquals("SALES", r.getReportType());
        assertEquals(start, r.getDateRangeStart());
        assertEquals(end, r.getDateRangeEnd());
        assertEquals("MGR001", r.getGeneratedBy());
    }

    @Test
    @DisplayName("Date range fields can be null")
    void dateRange_canBeNull() {
        Report r = new Report(2, LocalDateTime.now(), "INVENTORY",
                              null, null, "MGR001");

        assertNull(r.getDateRangeStart());
        assertNull(r.getDateRangeEnd());
    }

    @Test
    @DisplayName("Report type can be SALES, INVENTORY, or PRICE_CHANGE")
    void reportType_acceptsAllValues() {
        Report r = new Report();
        for (String type : new String[]{"SALES", "INVENTORY", "PRICE_CHANGE"}) {
            r.setReportType(type);
            assertEquals(type, r.getReportType());
        }
    }

    @Test
    @DisplayName("toString() contains key fields")
    void toString_containsKeyFields() {
        Report r = new Report(10, LocalDateTime.now(), "SALES",
                              LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), "MGR001");
        String str = r.toString();

        assertTrue(str.contains("10"));
        assertTrue(str.contains("SALES"));
        assertTrue(str.contains("MGR001"));
    }
}
