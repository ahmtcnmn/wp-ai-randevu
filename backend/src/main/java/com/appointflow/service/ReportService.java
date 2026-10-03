package com.appointflow.service;

import com.appointflow.dto.*;
import com.appointflow.entity.CampaignStatus;
import com.appointflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RandevuRepository randevuRepository;
    private final CustomerRepository customerRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final SlotCampaignRepository slotCampaignRepository;
    private final SegmentCampaignRepository segmentCampaignRepository;

    @Cacheable(value = "reports", key = "'dashboard:' + #tenantId")
    public DashboardReportResponse getDashboard(Long tenantId) {
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime from = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime to = thisMonth.atEndOfMonth().atTime(LocalTime.MAX);

        Long totalAppointmentsThisMonth = randevuRepository.countByTenantIdAndPeriod(tenantId, from, to);
        Double revenueThisMonth = randevuRepository.sumRevenueByPeriod(tenantId, from, to);
        Long newCustomersThisMonth = customerRepository.countNewByPeriod(tenantId, from, to);
        Long totalCustomers = customerRepository.countByTenantId(tenantId);
        Long activeCampaigns = slotCampaignRepository.countByTenantIdAndStatus(tenantId, CampaignStatus.ACTIVE);
        Double avgAppointmentValue = randevuRepository.avgToplamFiyatByTenant(tenantId);

        return new DashboardReportResponse(
                totalAppointmentsThisMonth,
                revenueThisMonth,
                newCustomersThisMonth,
                totalCustomers,
                activeCampaigns,
                avgAppointmentValue != null ? avgAppointmentValue : 0.0,
                LocalDateTime.now().toString()
        );
    }

    @Cacheable(value = "reports", key = "'appointments:' + #tenantId + ':' + #from + ':' + #to")
    public AppointmentReportResponse getAppointmentReport(Long tenantId, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        Long total = randevuRepository.countByTenantIdAndPeriod(tenantId, fromDt, toDt);

        Map<String, Long> byStatus = new HashMap<>();
        List<Object[]> statusRows = randevuRepository.countByStatusAndPeriod(tenantId, fromDt, toDt);
        for (Object[] row : statusRows) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }

        Map<Long, Double> revenueByStaff = new HashMap<>();
        List<Object[]> staffRows = randevuRepository.sumRevenueByStaff(tenantId, fromDt, toDt);
        for (Object[] row : staffRows) {
            if (row[0] != null) {
                revenueByStaff.put((Long) row[0], (Double) row[1]);
            }
        }

        return new AppointmentReportResponse(total, byStatus, revenueByStaff, from.toString(), to.toString());
    }

    @Cacheable(value = "reports", key = "'revenue:' + #tenantId + ':' + #from + ':' + #to")
    public RevenueReportResponse getRevenueReport(Long tenantId, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        Double totalRevenue = randevuRepository.sumRevenueByPeriod(tenantId, fromDt, toDt);

        Map<Long, Double> byStaff = new HashMap<>();
        List<Object[]> staffRows = randevuRepository.sumRevenueByStaff(tenantId, fromDt, toDt);
        for (Object[] row : staffRows) {
            if (row[0] != null) {
                byStaff.put((Long) row[0], (Double) row[1]);
            }
        }

        Map<String, Double> byService = new HashMap<>();
        List<Object[]> serviceRows = randevuRepository.sumRevenueByService(tenantId, fromDt, toDt);
        for (Object[] row : serviceRows) {
            if (row[0] != null) {
                byService.put(row[0].toString(), (Double) row[1]);
            }
        }

        Long totalCount = randevuRepository.countByTenantIdAndPeriod(tenantId, fromDt, toDt);
        double avg = (totalCount != null && totalCount > 0 && totalRevenue != null)
                ? totalRevenue / totalCount : 0.0;

        return new RevenueReportResponse(
                totalRevenue != null ? totalRevenue : 0.0,
                byStaff,
                byService,
                avg,
                from.toString(),
                to.toString()
        );
    }

    @Cacheable(value = "reports", key = "'customers:' + #tenantId")
    public CustomerReportResponse getCustomerReport(Long tenantId) {
        Long total = customerRepository.countByTenantId(tenantId);

        Map<String, Long> segmentDistribution = new HashMap<>();
        List<Object[]> segRows = customerSegmentRepository.countBySegmentType(tenantId);
        for (Object[] row : segRows) {
            segmentDistribution.put(row[0].toString(), (Long) row[1]);
        }

        YearMonth thisMonth = YearMonth.now();
        LocalDateTime from = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime to = thisMonth.atEndOfMonth().atTime(LocalTime.MAX);
        Long newCustomers = customerRepository.countNewByPeriod(tenantId, from, to);

        LocalDateTime churnCutoff = LocalDateTime.now().minusDays(90);
        Long churnRiskCount = customerRepository.countChurnRisk(tenantId, churnCutoff);

        // Returning = total - new this month (approximate)
        Long returningCustomers = total - newCustomers;

        // Blacklisted customers
        Long blacklistedCount = customerRepository.countBlacklisted(tenantId);

        return new CustomerReportResponse(
                total,
                segmentDistribution,
                newCustomers,
                returningCustomers > 0 ? returningCustomers : 0L,
                churnRiskCount,
                blacklistedCount
        );
    }

    @Cacheable(value = "reports", key = "'campaigns:' + #tenantId")
    public CampaignReportResponse getCampaignReport(Long tenantId) {
        List<Object[]> slotByStatusRows = slotCampaignRepository.countByTenantIdGroupByStatus(tenantId);
        Map<String, Long> slotByStatus = new HashMap<>();
        long slotTotal = 0;
        long slotFilled = 0;
        for (Object[] row : slotByStatusRows) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            slotByStatus.put(status, count);
            slotTotal += count;
            if ("FILLED".equals(status)) {
                slotFilled = count;
            }
        }

        double slotFillRate = slotTotal > 0 ? (double) slotFilled / slotTotal * 100.0 : 0.0;

        Long segmentCampaignsTotal = segmentCampaignRepository.countByTenantId(tenantId);

        return new CampaignReportResponse(
                slotTotal,
                slotFilled,
                slotFillRate,
                segmentCampaignsTotal,
                0.0, // conversion rate — future metric
                slotByStatus
        );
    }
}
