package com.appointflow.ai.handler;

import com.appointflow.ai.client.OpenAiApiClient;
import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.intent.IntentResult;
import com.appointflow.ai.service.AiConfigService;
import com.appointflow.dto.AppointmentRequest;
import com.appointflow.dto.AppointmentResponse;
import com.appointflow.dto.ServiceResponse;
import com.appointflow.dto.UserResponse;
import com.appointflow.conversation.entity.Conversation;
import com.appointflow.conversation.repository.ConversationRepository;
import com.appointflow.repository.CalismaSaatiRepository;
import com.appointflow.service.AppointmentService;
import com.appointflow.service.HizmetService;
import com.appointflow.service.SlotCalculationService;
import com.appointflow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlowHandler implements FlowHandler {

    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final java.util.regex.Pattern FAKE_CONFIRM_PATTERN = java.util.regex.Pattern.compile(
            "(kaydet|olusturdum|oluşturdum|olusturuyorum|oluşturuyorum|kaydediyorum|" +
                    "ayarlandi|ayarlandı|onaylandi|onaylandı|basariyla|başarıyla|" +
                    "randevu(n|nu)?z .*alindi|randevu(n|nu)?z .*alındı)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private final OpenAiApiClient openAiApiClient;
    private final AiConfigService aiConfigService;
    private final HizmetService hizmetService;
    private final UserService userService;
    private final SlotCalculationService slotCalculationService;
    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper;
    private final ConversationHistoryLoader historyLoader;
    private final ConversationRepository conversationRepository;
    private final CalismaSaatiRepository calismaSaatiRepository;
    private final com.appointflow.repository.ProductServiceSuggestionRepository productSuggestionRepository;

    @Override
    public String handle(String conversationId, String userMessage, IntentResult intentResult, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        List<ServiceResponse> hizmetler = hizmetService.getAll().stream()
                .filter(h -> Boolean.TRUE.equals(h.getAktif()))
                .toList();

        // Aktif çalışanları AI'a sun. Çalışma saati tanımlı değilse SlotCalculationService
        // varsayılan 09:00-18:00 (Pazar kapalı) kullanır — bu yüzden çalışma saati filtresi YOK.
        // OWNER, ADMIN, BRANCH_MANAGER, STAFF rolleri uzman olabilir (müşteri rolü hariç).
        List<UserResponse> calisanlar = userService.getAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getAktif()))
                .filter(c -> {
                    String r = c.getRol();
                    return "OWNER".equals(r) || "ADMIN".equals(r)
                            || "BRANCH_MANAGER".equals(r) || "STAFF".equals(r);
                })
                .toList();

        // Çalışan id → "Ad Soyad" map (prompt'ta kullanılacak)
        Map<Long, String> calisanAdMap = calisanlar.stream()
                .collect(Collectors.toMap(
                        UserResponse::getId,
                        c -> (c.getAd() + " " + c.getSoyad()).trim(),
                        (a, b) -> a));

        // Hizmet listesi: her hizmetin yanında YAPAB IL ECEK çalışan adları
        String hizmetListesi = hizmetler.stream()
                .map(h -> {
                    String fiyatBilgisi = Boolean.TRUE.equals(config.getFiyatBilgisiGoster())
                            ? " - " + h.getFiyat() + " TL" : "";
                    List<Long> yapabilenIds = h.getStaffIds() != null ? h.getStaffIds() : List.of();
                    String yapabilenler;
                    if (yapabilenIds.isEmpty()) {
                        yapabilenler = "tum calisanlar yapabilir";
                    } else {
                        yapabilenler = "SADECE " + yapabilenIds.stream()
                                .map(sid -> {
                                    String ad = calisanAdMap.get(sid);
                                    return ad != null ? String.format("%s (id=%d)", ad, sid) : null;
                                })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.joining(", "));
                    }
                    return String.format("- id=%d, ad=%s, sure=%d dk%s | Yapabilen: %s",
                            h.getId(), h.getAd(), h.getSureDakika(), fiyatBilgisi, yapabilenler);
                })
                .collect(Collectors.joining("\n"));

        // Çalışan listesi: her çalışanın yanında YAPABILECEGI hizmetler
        // Önce her çalışan için "yapabildiği hizmet adları" listesi çıkar
        Map<Long, List<String>> calisanHizmetleri = new java.util.HashMap<>();
        for (ServiceResponse h : hizmetler) {
            List<Long> hizmetinUzmanlari = h.getStaffIds() != null ? h.getStaffIds() : List.of();
            if (hizmetinUzmanlari.isEmpty()) {
                // Hizmette atama yoksa tüm çalışanlar yapabilir
                for (UserResponse c : calisanlar) {
                    calisanHizmetleri.computeIfAbsent(c.getId(), k -> new java.util.ArrayList<>()).add(h.getAd());
                }
            } else {
                for (Long uzmanId : hizmetinUzmanlari) {
                    calisanHizmetleri.computeIfAbsent(uzmanId, k -> new java.util.ArrayList<>()).add(h.getAd());
                }
            }
        }

        String calisanListesi = calisanlar.stream()
                .map(c -> {
                    List<String> yapabildigi = calisanHizmetleri.getOrDefault(c.getId(), List.of());
                    String hizmetStr = yapabildigi.isEmpty()
                            ? "(hicbir hizmet atanmamis)"
                            : String.join(", ", yapabildigi);
                    return String.format("- id=%d, ad=%s %s | Yapabildigi hizmetler: %s",
                            c.getId(), c.getAd(), c.getSoyad(), hizmetStr);
                })
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format("""
                %s

                Sen randevu alma akışını yöneten asistansın. Müşteri WhatsApp üzerinden seninle konuşuyor.

                MÜŞTERİ BİLGİSİ:
                - Adı: %s
                - Telefon: %s
                - customerId: %d

                MEVCUT HİZMETLER:
                %s

                MEVCUT ÇALIŞANLAR:
                %s

                BUGÜNÜN TARİHİ: %s

                KURALLAR:
                - Önce müşteriden hangi hizmeti istediğini, hangi gün ve saat tercih ettiğini öğren.
                - Eksik bilgi varsa nazikçe sor.

                ÇALIŞAN-HİZMET EŞLEŞMESİ (EN ÖNEMLİ KURAL — ASLA İHLAL ETME):
                - Her hizmetin yanında "Yapabilen: ..." kısmı yazılı. SADECE orada yazan çalışanlar o hizmeti yapabilir.
                - "Yapabilen: tum calisanlar yapabilir" yazıyorsa, o hizmeti tüm çalışanlar yapabilir.
                - "Yapabilen: SADECE Ali (id=3)" yazıyorsa, o hizmeti SADECE Ali yapabilir. BAŞKA UZMAN ÖNERME.
                - Müşteri "Bu hizmeti kim yapıyor?" diye sorarsa SADECE "Yapabilen:" satırındaki isimleri söyle. ASLA başka çalışan ismi sayma.
                - Müşteri yapamayan bir uzman isterse (örn: "Sakal Tıraşı'nı Mehmet yapsın" ama Sakal Tıraşı sadece Emircan'da) nazikçe şöyle de:
                  "Maalesef bu hizmeti sadece [yapabilen isimler] verebiliyor. Onlardan biriyle randevu ayarlayalım mı?"
                - "create_appointment" çağrısında uzman_id parametresine SADECE o hizmetin "Yapabilen:" listesindeki id'lerden birini gönder.

                - Müsait saatleri öğrenmek için "list_available_slots" toolunu kullan.
                - **ÇOK ÖNEMLİ:** Tool'dan dönen müsait saatleri MÜŞTERİYE GÖSTERİRKEN BİREBİR yaz, asla yuvarlama veya kısaltma yapma. Mesela tool "09:05, 09:20, 09:35" döndürdüyse sen de "09:05, 09:20, 09:35" yaz. Kendi kafandan "09:00" gibi saat URETME.
                - Müşteri saat seçerken sadece tool'un verdiği listeden bir saati seçmesini iste. Müşteri listede olmayan bir saat söylerse (örn: tool 12:05 verdi ama müşteri "12:00 olur" dedi), nazikçe en yakın müsait saati öner ve listede olan saatlerden birini seçmesini iste.
                - "create_appointment" toolunu çağırırken "saat" parametresini tool'un verdiği listeden birebir kullan. Asla listede olmayan bir saat gönderme.
                - Tüm bilgiler tamam olduğunda "create_appointment" toolunu çağırarak randevuyu OLUŞTUR. Sadece konuşma değil, gerçek randevu yarat.
                - Randevu oluşturulduktan sonra müşteriye onay mesajı ver (tarih, saat, hizmet, çalışan).
                - **ÜRÜN ÖNERİSİ:** Randevu oluşturulduktan sonra VEYA müşteri "X için ne ürün öneriyorsun" diye sorduğunda "recommend_products_for_service" toolunu hizmet_id ile çağır.
                  Tool boş liste dönerse ürün önerme. Dönen ürünleri isim ve fiyatla kısa öner (örn: "Yanında şu ürünleri kullanabilirsiniz: Saç Maskesi 150₺, Argan Yağı 200₺"). Asla ürün UYDURMA.
                - Türkçe, kısa ve net yanıt ver.
                """,
                config.getSistemPromptu(),
                ctx.getCustomerFullName() != null ? ctx.getCustomerFullName() : "Bilinmiyor",
                ctx.getCustomerPhone(),
                ctx.getCustomerId(),
                hizmetListesi.isBlank() ? "(Henüz hizmet tanımlanmamış)" : hizmetListesi,
                calisanListesi.isBlank() ? "(Henüz çalışan tanımlanmamış)" : calisanListesi,
                LocalDate.now()
        );

        List<OpenAiChatRequest.Tool> tools = buildTools();

        // Mevcut session'i (30dk bosluksuz blok) yukle — eski oturumlardan etkilenme
        List<OpenAiChatRequest.Message> messages = new ArrayList<>(
                historyLoader.loadCurrentSession(ctx.getConversationId(), 1));
        messages.add(OpenAiChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        boolean fakeDetected = false;
        boolean appointmentActuallyCreated = false;
        Long createdAppointmentId = null;
        String createdConfirmFallback = null;

        try {
            for (int iter = 0; iter < MAX_TOOL_ITERATIONS; iter++) {
                Object toolChoice = fakeDetected ? "required" : null;
                String activePrompt = fakeDetected
                        ? systemPrompt + "\n\nDIKKAT: Onceki yanitin tool cagirmadan randevu olusturmus gibi davraniyordu. Bu YASAK. SIMDI 'create_appointment' tool'unu MUTLAKA cagir."
                        : systemPrompt;

                OpenAiChatResponse response = openAiApiClient.sendMessage(
                        activePrompt, messages, "gpt-4o", config.getMaxToken(), tools, toolChoice);

                if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                    if (appointmentActuallyCreated) {
                        return createdConfirmFallback;
                    }
                    return "Üzgünüm, şu anda yanıt veremiyorum. Lütfen tekrar deneyin.";
                }

                if (!response.hasToolCalls()) {
                    String text = response.getTextContent();
                    // AI tool cagirmadan "basariyla/olusturdum" diyor + henuz gercek randevu yok → REDDET
                    if (!appointmentActuallyCreated && !fakeDetected && text != null
                            && FAKE_CONFIRM_PATTERN.matcher(text).find()) {
                        log.warn("AI fake confirm tespit edildi (tool yok): {}", text.substring(0, Math.min(100, text.length())));
                        fakeDetected = true;
                        continue;
                    }
                    return text;
                }

                List<OpenAiChatResponse.ToolCall> toolCalls = response.getToolCalls();

                List<OpenAiChatRequest.ToolCall> requestToolCalls = toolCalls.stream()
                        .map(tc -> OpenAiChatRequest.ToolCall.builder()
                                .id(tc.getId())
                                .type("function")
                                .function(OpenAiChatRequest.FunctionCall.builder()
                                        .name(tc.getFunction() != null ? tc.getFunction().getName() : null)
                                        .arguments(tc.getFunction() != null ? tc.getFunction().getArguments() : null)
                                        .build())
                                .build())
                        .toList();
                messages.add(OpenAiChatRequest.Message.builder()
                        .role("assistant")
                        .content(null)
                        .toolCalls(requestToolCalls)
                        .build());

                for (OpenAiChatResponse.ToolCall tc : toolCalls) {
                    Map<String, Object> input = tc.getInputAsMap();
                    String toolResult;

                    // KORUMA: bu turn'de zaten randevu olustuysa, tekrar create_appointment cagrisini reddet
                    if ("create_appointment".equals(tc.getName()) && appointmentActuallyCreated) {
                        log.warn("Cift randevu girisimi engellendi — zaten id={} olusturuldu", createdAppointmentId);
                        toolResult = "{\"ok\":false,\"error\":\"Bu turn'de zaten randevu (id=" + createdAppointmentId + ") olusturuldu. Yeni randevu olusturma.\"}";
                    } else {
                        toolResult = executeTool(tc.getName(), input, ctx);
                    }

                    log.info("Tool: {} input: {} result: {}",
                            tc.getName(), input,
                            toolResult.length() > 200 ? toolResult.substring(0, 200) + "..." : toolResult);

                    if ("create_appointment".equals(tc.getName()) && toolResult.contains("\"ok\":true")) {
                        appointmentActuallyCreated = true;
                        createdAppointmentId = extractRandevuId(toolResult);
                        createdConfirmFallback = buildConfirmFromToolResult(toolResult);
                    }
                    messages.add(OpenAiChatRequest.Message.builder()
                            .role("tool")
                            .toolCallId(tc.getId())
                            .content(toolResult)
                            .build());
                }
            }

            // MAX iter doldu — eger randevu olustuysa fallback teyit dondur, yoksa hata
            if (appointmentActuallyCreated) {
                log.info("MAX_TOOL_ITERATIONS doldu ama randevu olustu (id={}), fallback teyit donduruluyor", createdAppointmentId);
                return createdConfirmFallback;
            }
            return "Bir hata oluştu, lütfen tekrar deneyin.";

        } catch (Exception e) {
            // Akis ortasinda exception — eger randevu olustuysa rollback (iptal et)
            log.error("Booking akisinda hata: {}", e.getMessage(), e);
            if (appointmentActuallyCreated && createdAppointmentId != null) {
                try {
                    appointmentService.cancel(createdAppointmentId, "Sistem hatasi nedeniyle otomatik iptal");
                    log.warn("Otomatik rollback: randevu id={} iptal edildi", createdAppointmentId);
                    return "Randevu oluştururken bir sorun yaşandı, işlem geri alındı. Lütfen tekrar deneyin.";
                } catch (Exception rollbackEx) {
                    log.error("Rollback de basarisiz: {}", rollbackEx.getMessage());
                    return "Randevu oluştu ancak teyit edilemedi (id=" + createdAppointmentId + "). Lütfen işletmeyle iletişime geçin.";
                }
            }
            return "Bir hata oluştu, lütfen tekrar deneyin.";
        }
    }

    private Long extractRandevuId(String toolResult) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"randevu_id\"\\s*:\\s*(\\d+)").matcher(toolResult);
            if (m.find()) return Long.parseLong(m.group(1));
        } catch (Exception ignored) {}
        return null;
    }

    private String buildConfirmFromToolResult(String toolResult) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(toolResult, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Object tarihSaat = parsed.get("tarih_saat");
            Object uzman = parsed.get("uzman");
            Object fiyat = parsed.get("toplam_fiyat");
            return String.format("Randevunuz oluşturuldu ✅\n📅 %s\n👤 %s\n💰 %s TL",
                    tarihSaat, uzman, fiyat);
        } catch (Exception e) {
            return "Randevunuz başarıyla oluşturuldu ✅";
        }
    }

    private String executeTool(String name, Map<String, Object> input, ConversationContext ctx) {
        try {
            return switch (name) {
                case "list_available_slots" -> doListSlots(input);
                case "create_appointment" -> doCreateAppointment(input, ctx);
                case "recommend_products_for_service" -> doRecommendProducts(input);
                default -> jsonError("Bilinmeyen arac: " + name);
            };
        } catch (Exception e) {
            log.warn("Tool calistirma hatasi — tool: {}, hata: {}", name, e.getMessage());
            return jsonError(e.getMessage());
        }
    }

    private String doListSlots(Map<String, Object> input) {
        Long uzmanId = asLong(input.get("uzman_id"));
        List<Long> hizmetIds = asLongList(input.get("hizmet_ids"));
        LocalDate tarih = DateTimeParseUtil.parseDate(asString(input.get("tarih")));

        if (uzmanId == null || hizmetIds == null || hizmetIds.isEmpty() || tarih == null) {
            return jsonError("Eksik parametre: uzman_id, hizmet_ids veya tarih bos.");
        }

        // Hard guard: uzman bu hizmetleri gercekten yapabilir mi?
        String eslesmeHatasi = validateStaffServiceMatch(uzmanId, hizmetIds);
        if (eslesmeHatasi != null) return jsonError(eslesmeHatasi);

        List<LocalTime> slots = slotCalculationService.getAvailableSlots(uzmanId, hizmetIds, tarih);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tarih", tarih.toString());
        if (slots.isEmpty()) {
            result.put("musait_saatler", List.of());
            result.put("mesaj", "Bu tarihte musait saat bulunamadi.");
        } else {
            result.put("musait_saatler", slots.stream()
                    .map(t -> t.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .toList());
        }
        return toJson(result);
    }

    private String doCreateAppointment(Map<String, Object> input, ConversationContext ctx) {
        Long uzmanId = asLong(input.get("uzman_id"));
        List<Long> hizmetIds = asLongList(input.get("hizmet_ids"));
        LocalDate tarih = DateTimeParseUtil.parseDate(asString(input.get("tarih")));
        LocalTime saat = DateTimeParseUtil.parseTime(asString(input.get("saat")));
        String not = asString(input.get("not"));

        if (uzmanId == null || hizmetIds == null || hizmetIds.isEmpty() || tarih == null || saat == null) {
            return jsonError("Eksik parametre: uzman_id, hizmet_ids, tarih veya saat bos.");
        }

        // Hard guard: uzman bu hizmetleri gercekten yapabilir mi?
        String eslesmeHatasi = validateStaffServiceMatch(uzmanId, hizmetIds);
        if (eslesmeHatasi != null) return jsonError(eslesmeHatasi);

        // Snap: AI'nin verdigi saati gercek musait slot'a yuvarla (en yakin, <= 15 dakika tolerans)
        List<LocalTime> availableSlots = slotCalculationService.getAvailableSlots(uzmanId, hizmetIds, tarih);
        if (availableSlots.isEmpty()) {
            return jsonError("Bu tarih ve hizmet icin hic musait saat yok.");
        }
        LocalTime snapped = findClosestSlot(saat, availableSlots, 15);
        if (snapped == null) {
            String availableStr = availableSlots.stream()
                    .map(t -> t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.joining(", "));
            return jsonError("Saat " + saat + " civarinda musait slot yok. Musait saatler: " + availableStr);
        }
        if (!snapped.equals(saat)) {
            log.info("AI saati {} icin en yakin musait slot'a snap edildi: {}", saat, snapped);
            saat = snapped;
        }

        LocalDateTime tarihSaat = DateTimeParseUtil.combine(tarih, saat);
        if (tarihSaat == null || tarihSaat.isBefore(LocalDateTime.now())) {
            return jsonError("Gecmis tarihe randevu olusturulamaz.");
        }

        AppointmentRequest req = new AppointmentRequest();
        req.setCustomerId(ctx.getCustomerId());
        req.setUzmanId(uzmanId);
        req.setHizmetIds(hizmetIds);
        req.setTarihSaat(tarihSaat);
        req.setNot(not);
        req.setKaynak("WHATSAPP");

        AppointmentResponse created = appointmentService.create(req);

        // Randevu olusturuldu — aktif handler'i temizle (akis bitti)
        if (ctx.getConversationId() != null) {
            conversationRepository.findById(ctx.getConversationId()).ifPresent(c -> {
                c.setAktifHandler(null);
                conversationRepository.save(c);
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("randevu_id", created.getId());
        result.put("musteri", created.getMusteriAd());
        result.put("uzman", created.getUzmanAd());
        result.put("tarih_saat", created.getTarihSaat().toString());
        result.put("toplam_fiyat", created.getToplamFiyat());
        result.put("toplam_sure_dk", created.getToplamSureDk());
        return toJson(result);
    }

    private List<OpenAiChatRequest.Tool> buildTools() {
        Map<String, Object> slotSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "uzman_id", Map.of("type", "integer", "description", "Uzmanin id'si"),
                        "hizmet_ids", Map.of(
                                "type", "array",
                                "items", Map.of("type", "integer"),
                                "description", "Hizmet id listesi"
                        ),
                        "tarih", Map.of("type", "string", "description", "YYYY-MM-DD formatinda tarih veya 'bugun'/'yarin'")
                ),
                "required", List.of("uzman_id", "hizmet_ids", "tarih")
        );

        Map<String, Object> createSchema = Map.of(
                "type", "object",
                "properties", new LinkedHashMap<>(Map.of(
                        "uzman_id", Map.of("type", "integer"),
                        "hizmet_ids", Map.of("type", "array", "items", Map.of("type", "integer")),
                        "tarih", Map.of("type", "string", "description", "YYYY-MM-DD"),
                        "saat", Map.of("type", "string", "description", "HH:mm"),
                        "not", Map.of("type", "string", "description", "Istege bagli musteri notu")
                )),
                "required", List.of("uzman_id", "hizmet_ids", "tarih", "saat")
        );

        Map<String, Object> recommendSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "hizmet_id", Map.of("type", "integer", "description", "Önerilen ürünleri istediğiniz hizmetin id'si")
                ),
                "required", List.of("hizmet_id")
        );

        return List.of(
                OpenAiChatRequest.Tool.builder()
                        .type("function")
                        .function(OpenAiChatRequest.FunctionDef.builder()
                                .name("list_available_slots")
                                .description("Belirli bir uzman, hizmet listesi ve tarih icin musait saatleri getirir.")
                                .parameters(slotSchema)
                                .build())
                        .build(),
                OpenAiChatRequest.Tool.builder()
                        .type("function")
                        .function(OpenAiChatRequest.FunctionDef.builder()
                                .name("create_appointment")
                                .description("Tum parametreler hazir oldugunda gercek randevu olusturur.")
                                .parameters(createSchema)
                                .build())
                        .build(),
                OpenAiChatRequest.Tool.builder()
                        .type("function")
                        .function(OpenAiChatRequest.FunctionDef.builder()
                                .name("recommend_products_for_service")
                                .description("Belirli bir hizmet için işletmenin önerdiği ürünleri listeler. "
                                        + "Müşteri randevu sonrasında ürün isteyince veya 'X için ne öneriyorsun' diye sorunca kullan.")
                                .parameters(recommendSchema)
                                .build())
                        .build()
        );
    }

    /**
     * Uzmanin verilen hizmetleri yapabilir oldugunu kontrol eder.
     * Yapamiyorsa hata mesaji doner (AI bu mesaji musteriye iletir ve duzeltir).
     * Hizmetin staffIds listesi BOS ise tum calisanlar yapabilir kabul edilir.
     */
    private String validateStaffServiceMatch(Long uzmanId, List<Long> hizmetIds) {
        List<com.appointflow.dto.ServiceResponse> allServices = hizmetService.getAll();
        for (Long hid : hizmetIds) {
            com.appointflow.dto.ServiceResponse h = allServices.stream()
                    .filter(s -> s.getId().equals(hid))
                    .findFirst().orElse(null);
            if (h == null) continue;
            List<Long> yapabilenler = h.getStaffIds();
            // Atama yoksa herkes yapabilir
            if (yapabilenler == null || yapabilenler.isEmpty()) continue;
            if (!yapabilenler.contains(uzmanId)) {
                // Yapabilen isimleri liste haline getir
                java.util.Map<Long, String> userAdMap = userService.getAll().stream()
                        .collect(Collectors.toMap(
                                com.appointflow.dto.UserResponse::getId,
                                u -> (u.getAd() + " " + u.getSoyad()).trim(),
                                (a, b) -> a));
                String yapabilenAdlar = yapabilenler.stream()
                        .map(sid -> userAdMap.getOrDefault(sid, "id=" + sid))
                        .collect(Collectors.joining(", "));
                return String.format(
                        "'%s' hizmetini bu uzman (id=%d) yapmiyor. SADECE su uzmanlar yapabilir: %s. "
                        + "Lutfen musteriye bunu bildirin ve dogru uzmanla devam edin.",
                        h.getAd(), uzmanId, yapabilenAdlar);
            }
        }
        return null;
    }

    private String doRecommendProducts(Map<String, Object> input) {
        Long hizmetId = asLong(input.get("hizmet_id"));
        if (hizmetId == null) return jsonError("hizmet_id eksik.");

        var suggestions = productSuggestionRepository.findByHizmetIdAndProductActive(hizmetId);
        if (suggestions.isEmpty()) {
            return toJson(Map.of(
                    "ok", true,
                    "hizmet_id", hizmetId,
                    "urunler", List.of(),
                    "mesaj", "Bu hizmet için özel önerilen ürün tanımlanmamış."
            ));
        }
        List<Map<String, Object>> urunler = suggestions.stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getProduct().getId());
                    m.put("ad", s.getProduct().getAd());
                    m.put("fiyat", s.getProduct().getFiyat());
                    m.put("stok", s.getProduct().getStok());
                    return m;
                })
                .toList();
        return toJson(Map.of("ok", true, "hizmet_id", hizmetId, "urunler", urunler));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String jsonError(String msg) {
        return toJson(Map.of("ok", false, "error", msg != null ? msg : "unknown"));
    }

    /**
     * AI'nin verdigi saati gercek musait slot listesi icinde en yakin olana yuvarla.
     * Tolerans dakikasi disinda hicbir slot yoksa null doner.
     */
    private LocalTime findClosestSlot(LocalTime target, List<LocalTime> available, int toleranceMinutes) {
        LocalTime best = null;
        long bestDiff = Long.MAX_VALUE;
        for (LocalTime slot : available) {
            long diff = Math.abs(java.time.Duration.between(target, slot).toMinutes());
            if (diff <= toleranceMinutes && diff < bestDiff) {
                bestDiff = diff;
                best = slot;
            }
        }
        return best;
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private List<Long> asLongList(Object o) {
        if (o == null) return null;
        if (o instanceof List<?> list) {
            return list.stream()
                    .map(this::asLong)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return null;
    }
}
