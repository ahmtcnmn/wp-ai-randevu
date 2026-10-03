package com.appointflow.repository;

import com.appointflow.entity.Randevu;
import com.appointflow.entity.RandevuDurumu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RandevuRepository extends JpaRepository<Randevu, Long> {
    List<Randevu> findByMusteriId(Long musteriId);
    List<Randevu> findByUzmanId(Long uzmanId);
    List<Randevu> findByDurum(RandevuDurumu durum);
    List<Randevu> findByUzmanIdAndTarihSaatBetween(Long uzmanId, LocalDateTime baslangic, LocalDateTime bitis);
    List<Randevu> findByTenantId(Long tenantId);
    List<Randevu> findByCustomerId(Long customerId);

    List<Randevu> findByTenantIdAndCustomerId(Long tenantId, Long customerId);

    @Query("SELECT r FROM Randevu r WHERE r.tenantId = :tenantId AND r.customer.id = :customerId " +
           "AND r.durum = 'TAMAMLANDI' ORDER BY r.tarihSaat DESC")
    List<Randevu> findCompletedByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT COALESCE(AVG(r.toplamFiyat), 0.0) FROM Randevu r WHERE r.tenantId = :tenantId AND r.durum = 'TAMAMLANDI'")
    Double avgToplamFiyatByTenant(@Param("tenantId") Long tenantId);

    // Jobs: reminder queries
    List<Randevu> findByTenantIdAndTarihSaatBetweenAndDurum(Long tenantId, LocalDateTime from, LocalDateTime to, RandevuDurumu durum);

    // Reports: appointment count in period (IPTAL_EDILDI haric — kotaya sayilmaz)
    @Query("SELECT COUNT(r) FROM Randevu r WHERE r.tenantId = :tenantId " +
           "AND r.tarihSaat >= :from AND r.tarihSaat <= :to " +
           "AND r.durum <> com.appointflow.entity.RandevuDurumu.IPTAL_EDILDI")
    Long countByTenantIdAndPeriod(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    // Reports: revenue (TAMAMLANDI only)
    @Query("SELECT COALESCE(SUM(r.toplamFiyat), 0.0) FROM Randevu r WHERE r.tenantId = :tenantId " +
           "AND r.durum = 'TAMAMLANDI' AND r.tarihSaat >= :from AND r.tarihSaat <= :to")
    Double sumRevenueByPeriod(@Param("tenantId") Long tenantId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    // Reports: revenue by staff [uzmanId, sum]
    @Query("SELECT r.uzman.id, COALESCE(SUM(r.toplamFiyat), 0.0) FROM Randevu r WHERE r.tenantId = :tenantId " +
           "AND r.durum = 'TAMAMLANDI' AND r.tarihSaat >= :from AND r.tarihSaat <= :to " +
           "GROUP BY r.uzman.id")
    List<Object[]> sumRevenueByStaff(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    // Reports: count by status [durum, count]
    @Query("SELECT r.durum, COUNT(r) FROM Randevu r WHERE r.tenantId = :tenantId " +
           "AND r.tarihSaat >= :from AND r.tarihSaat <= :to GROUP BY r.durum")
    List<Object[]> countByStatusAndPeriod(@Param("tenantId") Long tenantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // Reports: revenue by service [hizmetId, sum]
    @Query("SELECT s.hizmet.id, COALESCE(SUM(s.fiyatSnapshot), 0.0) FROM AppointmentServiceEntity s " +
           "WHERE s.randevu.tenantId = :tenantId AND s.randevu.durum = 'TAMAMLANDI' " +
           "AND s.randevu.tarihSaat >= :from AND s.randevu.tarihSaat <= :to " +
           "GROUP BY s.hizmet.id")
    List<Object[]> sumRevenueByService(@Param("tenantId") Long tenantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(r) FROM Randevu r WHERE r.uzman.sube.id = :subeId " +
           "AND r.durum IN (com.appointflow.entity.RandevuDurumu.BEKLIYOR, com.appointflow.entity.RandevuDurumu.ONAYLANDI)")
    long countActiveBySubeId(@Param("subeId") Long subeId);

    @Query("SELECT COUNT(r) FROM Randevu r WHERE r.hizmet.id = :hizmetId " +
           "AND r.durum IN (com.appointflow.entity.RandevuDurumu.BEKLIYOR, com.appointflow.entity.RandevuDurumu.ONAYLANDI)")
    long countActiveByHizmetId(@Param("hizmetId") Long hizmetId);

    @Query("SELECT COUNT(r) FROM Randevu r WHERE r.uzman.id = :uzmanId " +
           "AND r.durum IN (com.appointflow.entity.RandevuDurumu.BEKLIYOR, com.appointflow.entity.RandevuDurumu.ONAYLANDI)")
    long countActiveByUzmanId(@Param("uzmanId") Long uzmanId);
}
