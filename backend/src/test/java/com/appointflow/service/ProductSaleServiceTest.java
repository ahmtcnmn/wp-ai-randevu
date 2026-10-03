package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ProductSaleRequest;
import com.appointflow.dto.ProductSaleResponse;
import com.appointflow.entity.*;
import com.appointflow.repository.EarningRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.ProductRepository;
import com.appointflow.repository.ProductSaleRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 6 + 6.1: Ürün satışı + komisyon lookup (6 seviye öncelik).
 */
@ExtendWith(MockitoExtension.class)
class ProductSaleServiceTest {

    @Mock ProductSaleRepository productSaleRepository;
    @Mock ProductRepository productRepository;
    @Mock RandevuRepository randevuRepository;
    @Mock KullaniciRepository kullaniciRepository;
    @Mock EarningRepository earningRepository;
    @Mock CommissionService commissionService;

    @InjectMocks ProductSaleService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void addSale_insufficientStock_throws400() {
        Randevu randevu = sampleRandevu();
        Product product = Product.builder()
                .id(5L).tenantId(1L).ad("Pomade").fiyat(new BigDecimal("100.00"))
                .stok(1).aktif(true).build();
        when(randevuRepository.findById(7L)).thenReturn(Optional.of(randevu));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductSaleRequest req = new ProductSaleRequest();
        req.setProductId(5L);
        req.setAdet(3);

        assertThatThrownBy(() -> service.addSaleToAppointment(7L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    @Test
    void addSale_inactiveProduct_throws400() {
        Randevu randevu = sampleRandevu();
        Product product = Product.builder()
                .id(5L).tenantId(1L).ad("Pomade").fiyat(new BigDecimal("100.00"))
                .stok(10).aktif(false).build();
        when(randevuRepository.findById(7L)).thenReturn(Optional.of(randevu));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductSaleRequest req = new ProductSaleRequest();
        req.setProductId(5L);
        req.setAdet(1);

        assertThatThrownBy(() -> service.addSaleToAppointment(7L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("aktif degil");
    }

    @Test
    void addSale_noCommissionRule_savesWithoutEarning() {
        Randevu randevu = sampleRandevu();
        Product product = Product.builder()
                .id(5L).tenantId(1L).ad("Pomade").kategori("Bakım")
                .fiyat(new BigDecimal("100.00")).stok(10).aktif(true).build();
        when(randevuRepository.findById(7L)).thenReturn(Optional.of(randevu));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(commissionService.findProductRule(1L, 3L, 5L, "Bakım")).thenReturn(Optional.empty());

        ArgumentCaptor<ProductSale> saleCaptor = ArgumentCaptor.forClass(ProductSale.class);
        when(productSaleRepository.save(any(ProductSale.class))).thenAnswer(inv -> {
            ProductSale s = inv.getArgument(0);
            s.setId(42L);
            return s;
        });

        ProductSaleRequest req = new ProductSaleRequest();
        req.setProductId(5L);
        req.setAdet(2);
        ProductSaleResponse resp = service.addSaleToAppointment(7L, req);

        verify(productSaleRepository).save(saleCaptor.capture());
        ProductSale saved = saleCaptor.getValue();
        assertThat(saved.getToplamTutar()).isEqualByComparingTo("200.00");
        assertThat(saved.getCommissionAmount()).isNull();

        // Stok düşürüldü
        assertThat(product.getStok()).isEqualTo(8);
        verify(productRepository).save(product);

        // Earning oluşturulmadı
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void addSale_withRule_createsEarning() {
        Randevu randevu = sampleRandevu();
        Product product = Product.builder()
                .id(5L).tenantId(1L).ad("Pomade").kategori("Bakım")
                .fiyat(new BigDecimal("100.00")).stok(10).aktif(true).build();
        CommissionRule rule = CommissionRule.builder()
                .id(11L).tenantId(1L).staffId(3L).productId(5L)
                .scope(CommissionScope.PRODUCT)
                .commissionType(CommissionType.PERCENTAGE)
                .rate(new BigDecimal("0.30"))
                .aktif(true).build();

        when(randevuRepository.findById(7L)).thenReturn(Optional.of(randevu));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(commissionService.findProductRule(1L, 3L, 5L, "Bakım")).thenReturn(Optional.of(rule));
        when(commissionService.calculateCommissionAmount(rule, new BigDecimal("200.00")))
                .thenReturn(new BigDecimal("60.00"));
        when(productSaleRepository.save(any(ProductSale.class))).thenAnswer(inv -> {
            ProductSale s = inv.getArgument(0);
            s.setId(42L);
            return s;
        });

        ProductSaleRequest req = new ProductSaleRequest();
        req.setProductId(5L);
        req.setAdet(2);
        service.addSaleToAppointment(7L, req);

        ArgumentCaptor<Earning> earningCaptor = ArgumentCaptor.forClass(Earning.class);
        verify(earningRepository).save(earningCaptor.capture());
        Earning earning = earningCaptor.getValue();
        assertThat(earning.getProductSaleId()).isEqualTo(42L);
        assertThat(earning.getStaffId()).isEqualTo(3L);
        assertThat(earning.getGrossAmount()).isEqualByComparingTo("200.00");
        assertThat(earning.getCommissionAmount()).isEqualByComparingTo("60.00");
        assertThat(earning.getNetAmount()).isEqualByComparingTo("140.00");
        assertThat(earning.getStatus()).isEqualTo(EarningStatus.PENDING);
    }

    private Randevu sampleRandevu() {
        Kullanici uzman = Kullanici.builder().id(3L).tenantId(1L).ad("Ahmet").soyad("Yılmaz").build();
        Customer customer = Customer.builder().id(10L).tenantId(1L).ad("Ali").soyad("Veli").build();
        return Randevu.builder()
                .id(7L).tenantId(1L)
                .uzman(uzman).customer(customer)
                .build();
    }
}
