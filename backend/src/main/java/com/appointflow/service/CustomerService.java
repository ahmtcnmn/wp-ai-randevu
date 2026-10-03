package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.CustomerRequest;
import com.appointflow.dto.CustomerResponse;
import com.appointflow.entity.Customer;
import com.appointflow.entity.CustomerTag;
import com.appointflow.repository.CustomerRepository;
import com.appointflow.repository.CustomerTagRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTagRepository tagRepository;
    private final RandevuRepository randevuRepository;

    public List<CustomerResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return customerRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CustomerResponse getById(Long id) {
        return toResponse(findByIdAndTenant(id));
    }

    @CacheEvict(value = "reports", allEntries = true)
    public CustomerResponse create(CustomerRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (customerRepository.existsByTenantIdAndTelefon(tenantId, request.getTelefon())) {
            throw ApiException.conflict("Bu telefon numarasi ile kayitli musteri var.");
        }

        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .ad(request.getAd())
                .soyad(request.getSoyad())
                .telefon(request.getTelefon())
                .email(request.getEmail())
                .notlar(request.getNotlar())
                .build();
        customerRepository.save(customer);
        return toResponse(customer);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findByIdAndTenant(id);
        customer.setAd(request.getAd());
        customer.setSoyad(request.getSoyad());
        customer.setTelefon(request.getTelefon());
        customer.setEmail(request.getEmail());
        customer.setNotlar(request.getNotlar());
        customerRepository.save(customer);
        return toResponse(customer);
    }

    public Long findByTelefon(Long tenantId, String telefon) {
        return customerRepository.findByTenantIdAndTelefon(tenantId, telefon)
                .map(Customer::getId)
                .orElseThrow(() -> ApiException.notFound("Musteri bulunamadi: " + telefon));
    }

    @CacheEvict(value = "reports", allEntries = true)
    public void delete(Long id) {
        Customer customer = findByIdAndTenant(id);
        if (!randevuRepository.findByCustomerId(id).isEmpty()) {
            throw ApiException.badRequest("Randevusu olan musteri silinemez. Once randevulari iptal edin.");
        }
        customerRepository.delete(customer);
    }

    public void block(Long id) {
        Customer customer = findByIdAndTenant(id);
        customer.setKaraListedeMi(true);
        customerRepository.save(customer);
    }

    public void unblock(Long id) {
        Customer customer = findByIdAndTenant(id);
        customer.setKaraListedeMi(false);
        customerRepository.save(customer);
    }

    public CustomerResponse addTag(Long customerId, String etiket) {
        Customer customer = findByIdAndTenant(customerId);
        CustomerTag tag = CustomerTag.builder()
                .tenantId(TenantContext.getTenantId())
                .customer(customer)
                .etiket(etiket)
                .build();
        tagRepository.save(tag);
        return toResponse(customer);
    }

    public void removeTag(Long customerId, Long tagId) {
        findByIdAndTenant(customerId); // yetki kontrolu
        CustomerTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> ApiException.notFound("Etiket bulunamadi."));
        tagRepository.delete(tag);
    }

    private Customer findByIdAndTenant(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Musteri bulunamadi."));
        if (!c.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu musteriye erisim yetkiniz yok.");
        }
        return c;
    }

    private CustomerResponse toResponse(Customer c) {
        List<String> etiketler = tagRepository.findByCustomerId(c.getId()).stream()
                .map(CustomerTag::getEtiket)
                .collect(Collectors.toList());

        return CustomerResponse.builder()
                .id(c.getId())
                .ad(c.getAd())
                .soyad(c.getSoyad())
                .telefon(c.getTelefon())
                .email(c.getEmail())
                .notlar(c.getNotlar())
                .gelmemeSayisi(c.getGelmemeSayisi())
                .karaListedeMi(c.getKaraListedeMi())
                .sadakatPuani(c.getSadakatPuani())
                .sonZiyaret(c.getSonZiyaret())
                .etiketler(etiketler)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
