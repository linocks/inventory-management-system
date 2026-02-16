package com.inventory.reporting.controller;

import com.inventory.reporting.dto.InventoryReportDTO;
import com.inventory.reporting.dto.StockLevelDTO;
import com.inventory.reporting.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/reports/stock-levels")
    class GetStockLevels {

        @Test
        @DisplayName("should return all stock levels")
        void shouldReturnStockLevels() throws Exception {
            StockLevelDTO dto = StockLevelDTO.builder()
                    .productId(1L)
                    .sku("PROD-001")
                    .quantity(50)
                    .minThreshold(10)
                    .lowStock(false)
                    .build();
            Page<StockLevelDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
            when(reportService.getAllStockLevels(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/reports/stock-levels")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].sku").value("PROD-001"))
                    .andExpect(jsonPath("$.data.content[0].lowStock").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/low-stock")
    class GetLowStock {

        @Test
        @DisplayName("should return low stock products")
        void shouldReturnLowStock() throws Exception {
            StockLevelDTO dto = StockLevelDTO.builder()
                    .productId(1L)
                    .sku("PROD-002")
                    .quantity(3)
                    .minThreshold(10)
                    .lowStock(true)
                    .build();
            Page<StockLevelDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
            when(reportService.getLowStockProducts(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/reports/low-stock")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].lowStock").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/summary")
    class GetSummary {

        @Test
        @DisplayName("should return inventory summary")
        void shouldReturnSummary() throws Exception {
            InventoryReportDTO summary = InventoryReportDTO.builder()
                    .totalProducts(10)
                    .totalStockUnits(500)
                    .lowStockProducts(2)
                    .outOfStockProducts(1)
                    .build();
            when(reportService.getInventorySummary()).thenReturn(summary);

            mockMvc.perform(get("/api/v1/reports/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalProducts").value(10))
                    .andExpect(jsonPath("$.data.totalStockUnits").value(500))
                    .andExpect(jsonPath("$.data.lowStockProducts").value(2))
                    .andExpect(jsonPath("$.data.outOfStockProducts").value(1));
        }
    }
}
