package com.appointflow.ai.tool;

import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.entity.RandevuDurumu;
import com.appointflow.repository.CustomerRepository;
import com.appointflow.repository.HizmetRepository;
import com.appointflow.repository.ProductServiceSuggestionRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.repository.ProductRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel asistanının kullanabileceği OpenAI tool'ları.
 *
 * Her tool: (1) OpenAI'ye bildirilen function schema (definitions()), (2) execute() implementasyonu.
 * Tüm tool'lar TenantContext'ten tenantId okur — kullanıcı kendi tenant'ı dışına sızamaz.
 *
 * Yeni tool eklerken:
 *  1. definitions() listesine FunctionDef ekle.
 *  2. execute(name, args) switch'ine case ekle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PanelAssistantTools {

    private final RandevuRepository randevuRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final HizmetRepository hizmetRepository;
    private final ProductServiceSuggestionRepository suggestionRepository;

    public List<OpenAiChatRequest.Tool> definitions() {
        return List.of(
                tool("count_appointments_this_week",
                        "Bu haftaki (Pzt-Paz) randevu sayısı. Parametre yok.",
                        emptySchema()),
                tool("count_appointments_today",
                        "Bugünkü randevu sayısı. Parametre yok.",
                        emptySchema()),
                tool("get_today_revenue",
                        "Bugün tamamlanan randevuların toplam cirosu (TRY). Parametre yok.",
                        emptySchema()),
                tool("get_month_revenue",
                        "İçinde bulunulan ayın tamamlanan randevu cirosu (TRY). Parametre yok.",
                        emptySchema()),
                tool("count_pending_appointments",
                        "Henüz tamamlanmamış (BEKLIYOR/ONAYLANDI) randevu sayısı. Parametre yok.",
                        emptySchema()),
                tool("find_customer_by_name",
                        "Müşteriyi ad/soyad ile arar — eşleşen ilk 5 müşterinin ad ve telefonunu döner.",
                        objSchema(Map.of("name", strProp("Aranan müşteri adı veya soyadı"))), "name"),
                tool("count_total_customers",
                        "Toplam aktif müşteri sayısı. Parametre yok.",
                        emptySchema()),
                tool("count_low_stock_products",
                        "Stoğu 5'in altında olan ürün sayısı ve listesi (ilk 10). Parametre yok.",
                        emptySchema()),
                tool("get_top_services_this_month",
                        "Bu ay en çok randevu alan ilk 5 hizmet — adet ile.",
                        emptySchema()),
                tool("count_completed_appointments_this_month",
                        "Bu ay tamamlanan (TAMAMLANDI) randevu sayısı. Parametre yok.",
                        emptySchema()),
                tool("get_recommended_products_for_service",
                        "Bir hizmet adına göre o hizmette önerilecek ürünleri listeler. " +
                        "Örneğin müşteri 'saç bakımı için ne kullanmalıyım' diye sorduğunda kullan.",
                        objSchema(Map.of("serviceName", strProp("Hizmet adı, örn: Saç Bakımı"))), "serviceName")
        );
    }

    /**
     * Tool çalıştır — sonucu OpenAI'ye gönderilecek string olarak döner.
     * Hata durumunda kullanıcıya doğal bir özür mesajı döner; exception fırlatmaz.
     */
    public String execute(String toolName, Map<String, Object> args) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return "Hata: Oturum bilgisi okunamadı.";

        try {
            return switch (toolName) {
                case "count_appointments_this_week" -> countAppointmentsThisWeek(tenantId);
                case "count_appointments_today" -> countAppointmentsToday(tenantId);
                case "get_today_revenue" -> getTodayRevenue(tenantId);
                case "get_month_revenue" -> getMonthRevenue(tenantId);
                case "count_pending_appointments" -> countPendingAppointments(tenantId);
                case "find_customer_by_name" -> findCustomerByName(tenantId, str(args, "name"));
                case "count_total_customers" -> "Toplam müşteri: " + customerRepository.findByTenantId(tenantId).size();
                case "count_low_stock_products" -> countLowStock(tenantId);
                case "get_top_services_this_month" -> getTopServices(tenantId);
                case "count_completed_appointments_this_month" -> completedThisMonth(tenantId);
                case "get_recommended_products_for_service" -> recommendedProductsForService(tenantId, str(args, "serviceName"));
                default -> "Bilinmeyen araç: " + toolName;
            };
        } catch (Exception e) {
            log.warn("Tool execute hatasi: {} — {}", toolName, e.getMessage());
            return "Bu bilgiye şu an erişemedim, lütfen ilgili sayfadan kontrol edin.";
        }
    }

    // ─── Tool Implementasyonları ───────────────────────────────────────────────

    private String countAppointmentsThisWeek(Long tenantId) {
        LocalDateTime start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime end = start.plusDays(7);
        long count = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .count();
        return "Bu hafta toplam " + count + " randevu var.";
    }

    private String countAppointmentsToday(Long tenantId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long count = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .count();
        return "Bugün " + count + " randevu var.";
    }

    private String getTodayRevenue(Long tenantId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        double total = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.TAMAMLANDI)
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .mapToDouble(r -> r.getToplamFiyat() != null ? r.getToplamFiyat() : 0.0)
                .sum();
        return "Bugünkü ciro: " + String.format("%.2f", total) + " TRY.";
    }

    private String getMonthRevenue(Long tenantId) {
        YearMonth ym = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        double total = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.TAMAMLANDI)
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .mapToDouble(r -> r.getToplamFiyat() != null ? r.getToplamFiyat() : 0.0)
                .sum();
        return "Bu ayın cirosu: " + String.format("%.2f", total) + " TRY.";
    }

    private String countPendingAppointments(Long tenantId) {
        long count = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.BEKLIYOR || r.getDurum() == RandevuDurumu.ONAYLANDI)
                .count();
        return "Bekleyen/onaylı randevu sayısı: " + count + ".";
    }

    private String findCustomerByName(Long tenantId, String name) {
        if (name == null || name.isBlank()) return "Lütfen aranacak isim verin.";
        String lower = name.toLowerCase();
        List<String> results = customerRepository.findByTenantId(tenantId).stream()
                .filter(c -> (c.getAd() != null && c.getAd().toLowerCase().contains(lower))
                        || (c.getSoyad() != null && c.getSoyad().toLowerCase().contains(lower)))
                .limit(5)
                .map(c -> String.format("%s %s (%s)", c.getAd(), c.getSoyad(), c.getTelefon()))
                .toList();
        if (results.isEmpty()) return "\"" + name + "\" için müşteri bulunamadı.";
        return "Eşleşen müşteriler:\n• " + String.join("\n• ", results);
    }

    private String countLowStock(Long tenantId) {
        var list = productRepository.findByTenantId(tenantId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getAktif()) && p.getStok() != null && p.getStok() < 5)
                .toList();
        if (list.isEmpty()) return "Stok 5'in altında ürün yok.";
        String urunler = list.stream()
                .limit(10)
                .map(p -> p.getAd() + " (stok: " + p.getStok() + ")")
                .reduce((a, b) -> a + ", " + b).orElse("");
        return list.size() + " ürün düşük stokta: " + urunler;
    }

    private String getTopServices(Long tenantId) {
        YearMonth ym = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        Map<String, Long> counter = new LinkedHashMap<>();
        randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .filter(r -> r.getHizmet() != null && r.getHizmet().getAd() != null)
                .forEach(r -> counter.merge(r.getHizmet().getAd(), 1L, Long::sum));
        if (counter.isEmpty()) return "Bu ay hizmet bazlı veri yok.";
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + " (" + e.getValue() + " randevu)")
                .reduce("Bu ayın en çok alınan hizmetleri:\n• ",
                        (acc, s) -> acc.equals("Bu ayın en çok alınan hizmetleri:\n• ") ? acc + s : acc + "\n• " + s);
    }

    private String recommendedProductsForService(Long tenantId, String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return "Hizmet adı belirtilmedi.";
        String lower = serviceName.toLowerCase();
        var matchingService = hizmetRepository.findByTenantId(tenantId).stream()
                .filter(h -> h.getAd() != null && h.getAd().toLowerCase().contains(lower))
                .findFirst()
                .orElse(null);
        if (matchingService == null) return "\"" + serviceName + "\" adında hizmet bulunamadı.";
        var products = suggestionRepository.findByHizmetIdAndProductActive(matchingService.getId());
        if (products.isEmpty()) {
            return matchingService.getAd() + " hizmeti için önerilen ürün tanımlanmamış. " +
                    "Ürünler sayfasından bir ürünü bu hizmete bağlayabilirsiniz.";
        }
        String urunler = products.stream()
                .map(s -> s.getProduct().getAd() + " (" + s.getProduct().getFiyat() + " TRY)")
                .reduce((a, b) -> a + ", " + b).orElse("");
        return matchingService.getAd() + " için önerilen ürünler: " + urunler;
    }

    private String completedThisMonth(Long tenantId) {
        YearMonth ym = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        long count = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.TAMAMLANDI)
                .filter(r -> r.getTarihSaat() != null
                        && !r.getTarihSaat().isBefore(start)
                        && r.getTarihSaat().isBefore(end))
                .count();
        return "Bu ay tamamlanan randevu sayısı: " + count + ".";
    }

    // ─── Schema Helpers ────────────────────────────────────────────────────────

    private static OpenAiChatRequest.Tool tool(String name, String description,
                                                Map<String, Object> schema, String... required) {
        if (required.length > 0) {
            schema.put("required", List.of(required));
        }
        return OpenAiChatRequest.Tool.builder()
                .type("function")
                .function(OpenAiChatRequest.FunctionDef.builder()
                        .name(name)
                        .description(description)
                        .parameters(schema)
                        .build())
                .build();
    }

    private static Map<String, Object> emptySchema() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("properties", Map.of());
        return m;
    }

    private static Map<String, Object> objSchema(Map<String, Object> props) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("properties", props);
        return m;
    }

    private static Map<String, Object> strProp(String desc) {
        return Map.of("type", "string", "description", desc);
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }
}
