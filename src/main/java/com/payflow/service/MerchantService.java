
package com.payflow.service;

import com.payflow.entity.Merchant;
import com.payflow.repository.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Locale;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    // Create a merchant
    public Merchant createMerchant(String name, String email) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant name is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant email is required"
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (merchantRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "Merchant email already exists"
            );
        }

        Merchant merchant = new Merchant(
                name.trim(),
                normalizedEmail
        );

        return merchantRepository.save(merchant);
    }

    // Get merchant by ID
    public Merchant getMerchantById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Merchant not found: " + id
                        )
                );
    }

    // Get all merchants
    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    // Update a merchant
    public Merchant updateMerchant(UUID id, String name, String email) {

        Merchant merchant = getMerchantById(id);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant name is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant email is required"
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!merchant.getEmail().equals(normalizedEmail)
                && merchantRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "Merchant email already exists"
            );
        }

        merchant.setName(name.trim());
        merchant.setEmail(normalizedEmail);

        return merchantRepository.save(merchant);
    }

    // Delete a merchant
    public void deleteMerchant(UUID id) {
        Merchant merchant = getMerchantById(id);
        merchantRepository.delete(merchant);
    }
}
