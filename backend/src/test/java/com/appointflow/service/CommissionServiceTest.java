package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.CommissionRuleRequest;
import com.appointflow.entity.CommissionRule;
import com.appointflow.entity.CommissionScope;
import com.appointflow.entity.CommissionType;
import com.appointflow.repository.CommissionRuleRepository;
import com.appointflow.repository.EarningPeriodRepository;
import com.appointflow.repository.EarningRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Sprint 6 + 6.1: scope validation + 6-seviye PRODUCT kural lookup.
 */
@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock CommissionRuleRepository commissionRuleRepository;
    @Mock EarningRepository earningRepository;
    @Mock EarningPeriodRepository earningPeriodRepository;
    @Mock KullaniciRepository kullaniciRepository;

    @InjectMocks CommissionService service;

    @BeforeEach
    void setUp() { TenantContext.set(1L); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void createRule_serviceScopeWithProductId_throws400() {
        CommissionRuleRequest req = new CommissionRuleRequest();
        req.setCommissionType("PERCENTAGE");
        req.setRate(new BigDecimal("0.40"));
        req.setStaffId(3L);
        req.setScope("SERVICE");
        req.setProductId(5L); // INVALID — SERVICE scope with productId

        assertThatThrownBy(() -> service.createRule(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("SERVICE scope kurallarinda");
    }

    @Test
    void createRule_productIdAndKategoriBothSet_throws400() {
        CommissionRuleRequest req = new CommissionRuleRequest();
        req.setCommissionType("PERCENTAGE");
        req.setRate(new BigDecimal("0.20"));
        req.setScope("PRODUCT");
        req.setProductId(5L);
        req.setProductKategori("Bakım");

        assertThatThrownBy(() -> service.createRule(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ayni anda hem productId hem productKategori");
    }

    @Test
    void createRule_duplicateActiveKey_throws400() {
        CommissionRuleRequest req = new CommissionRuleRequest();
        req.setCommissionType("PERCENTAGE");
        req.setRate(new BigDecimal("0.10"));
        req.setScope("PRODUCT");
        req.setStaffId(3L);
        req.setProductId(5L);

        when(commissionRuleRepository.existsActiveRuleForKey(1L, CommissionScope.PRODUCT, 3L, 5L, null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRule(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("zaten aktif");
    }

    @Test
    void findProductRule_picksStaffProductFirst() {
        CommissionRule staffProd = CommissionRule.builder()
                .id(1L).scope(CommissionScope.PRODUCT)
                .staffId(3L).productId(5L)
                .rate(new BigDecimal("0.30")).build();

        when(commissionRuleRepository.findStaffProductRule(1L, 3L, 5L))
                .thenReturn(Optional.of(staffProd));

        Optional<CommissionRule> result = service.findProductRule(1L, 3L, 5L, "Bakım");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findProductRule_fallsBackToTenantKategoriIfStaffSpecificNotFound() {
        CommissionRule tenantKat = CommissionRule.builder()
                .id(4L).scope(CommissionScope.PRODUCT)
                .staffId(null).productKategori("Bakım")
                .rate(new BigDecimal("0.18")).build();

        // 1. Staff + product yok
        when(commissionRuleRepository.findStaffProductRule(1L, 3L, 5L)).thenReturn(Optional.empty());
        // 2. Staff + kategori yok
        when(commissionRuleRepository.findStaffKategoriRule(1L, 3L, "Bakım")).thenReturn(Optional.empty());
        // 3. Tenant + product yok
        when(commissionRuleRepository.findTenantProductRule(1L, 5L)).thenReturn(Optional.empty());
        // 4. Tenant + kategori VAR
        when(commissionRuleRepository.findTenantKategoriRule(1L, "Bakım")).thenReturn(Optional.of(tenantKat));

        Optional<CommissionRule> result = service.findProductRule(1L, 3L, 5L, "Bakım");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(4L);
    }

    @Test
    void findProductRule_noMatch_returnsEmpty() {
        when(commissionRuleRepository.findStaffProductRule(1L, 3L, 5L)).thenReturn(Optional.empty());
        when(commissionRuleRepository.findStaffKategoriRule(1L, 3L, "Bakım")).thenReturn(Optional.empty());
        when(commissionRuleRepository.findTenantProductRule(1L, 5L)).thenReturn(Optional.empty());
        when(commissionRuleRepository.findTenantKategoriRule(1L, "Bakım")).thenReturn(Optional.empty());
        when(commissionRuleRepository.findStaffGeneralRule(1L, 3L, CommissionScope.PRODUCT)).thenReturn(Optional.empty());
        when(commissionRuleRepository.findTenantDefaultRule(1L, CommissionScope.PRODUCT)).thenReturn(Optional.empty());

        Optional<CommissionRule> result = service.findProductRule(1L, 3L, 5L, "Bakım");

        assertThat(result).isEmpty();
    }

    @Test
    void calculateCommissionAmount_percentage() {
        CommissionRule rule = CommissionRule.builder()
                .commissionType(CommissionType.PERCENTAGE)
                .rate(new BigDecimal("0.30"))
                .build();
        BigDecimal result = service.calculateCommissionAmount(rule, new BigDecimal("200.00"));
        assertThat(result).isEqualByComparingTo("60.00");
    }

    @Test
    void calculateCommissionAmount_salaryBonus_belowThreshold_zero() {
        CommissionRule rule = CommissionRule.builder()
                .commissionType(CommissionType.SALARY_PLUS_BONUS)
                .rate(new BigDecimal("0.10"))
                .bonusThreshold(new BigDecimal("500.00"))
                .build();
        BigDecimal result = service.calculateCommissionAmount(rule, new BigDecimal("200.00"));
        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    void calculateCommissionAmount_salaryBonus_aboveThreshold_pays() {
        CommissionRule rule = CommissionRule.builder()
                .commissionType(CommissionType.SALARY_PLUS_BONUS)
                .rate(new BigDecimal("0.10"))
                .bonusThreshold(new BigDecimal("500.00"))
                .build();
        BigDecimal result = service.calculateCommissionAmount(rule, new BigDecimal("700.00"));
        assertThat(result).isEqualByComparingTo("70.00");
    }
}
