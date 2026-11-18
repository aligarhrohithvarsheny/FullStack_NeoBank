package com.neo.springapp.service;

import com.neo.springapp.model.CreditCardProduct;
import com.neo.springapp.repository.CreditCardProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CreditCardProductCatalogService {
    private static final String DEFAULT_ELIGIBILITY =
            "Applicants must meet NeoBank identity, income and credit assessment requirements. Approval is subject to verification.";
    private static final String DEFAULT_TERMS =
            "Credit facilities are subject to approval. Fees, interest, repayment obligations and card usage are governed by the final NeoBank card agreement. Review the agreement before accepting an offer.";
    private static final List<ProductView> DEFAULT_PRODUCTS = List.of(
            defaultProduct("ZENITH_PLUS", "Zenith+ Credit Card", "Premium privileges for every day",
                    "emerald", 0, List.of("Metal Credit Card", "Grand Welcome", "Taj Epicure Membership")),
            defaultProduct("ANANTA", "Ananta Credit Card", "Benefits that travel with you",
                    "gold", 1, List.of("Up to 15% discount on merchants", "Exciting travel benefits", "Accelerated reward points")),
            defaultProduct("LAKSYA", "Laksya Credit Card", "A little more from every purchase",
                    "ocean", 2, List.of("Up to 15% discount on merchants", "Complimentary airport lounge access", "Accelerated reward points")),
            defaultProduct("TEJAS", "Tejas Credit Card", "Reward your everyday spending",
                    "sunset", 3, List.of("5 reward points on AU Rewardz", "Up to 10% cashback", "Instant reward on activation")),
            defaultProduct("PRATHAMA", "Prathama Credit Card", "More value with every tap",
                    "ruby", 4, List.of("Up to 2 reward points on tap and pay", "500 bonus reward points on recurring payment", "1% fuel surcharge waiver")),
            defaultProduct("LIT", "LIT Credit Card", "Choose features for your lifestyle",
                    "lilac", 5, List.of("Choose features as per your lifestyle", "Pay for the features used", "Switch features on or off when needed"))
    );

    private final CreditCardProductRepository repository;

    public CreditCardProductCatalogService(CreditCardProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProductView> getPublicProducts() {
        List<CreditCardProduct> products = repository.findAllByOrderBySortOrderAscNameAsc();
        if (products.isEmpty()) return DEFAULT_PRODUCTS;
        return products.stream().filter(CreditCardProduct::isActive).map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> getAdminProducts() {
        List<CreditCardProduct> products = repository.findAllByOrderBySortOrderAscNameAsc();
        if (products.isEmpty()) return DEFAULT_PRODUCTS;
        return products.stream().map(this::toView).toList();
    }

    @Transactional
    public List<ProductView> saveAll(List<ProductInput> inputs, String adminName) {
        if (inputs == null || inputs.isEmpty() || inputs.size() > 30) {
            throw new IllegalArgumentException("Add between 1 and 30 credit card products before saving.");
        }
        Set<String> productCodes = new HashSet<>();
        for (ProductInput input : inputs) {
            validate(input);
            String code = input.productCode().trim().toUpperCase(Locale.ROOT);
            if (!productCodes.add(code)) {
                throw new IllegalArgumentException("Product codes must be unique.");
            }
        }

        List<CreditCardProduct> existing = repository.findAllByOrderBySortOrderAscNameAsc();
        existing.forEach(product -> product.setActive(false));
        repository.saveAll(existing);

        List<CreditCardProduct> saved = new ArrayList<>();
        for (ProductInput input : inputs) {
            String code = input.productCode().trim().toUpperCase(Locale.ROOT);
            CreditCardProduct product = repository.findByProductCodeIgnoreCase(code)
                    .orElseGet(CreditCardProduct::new);
            product.setProductCode(code);
            product.setName(input.name().trim());
            product.setDescription(input.description().trim());
            product.setTheme(input.theme().trim().toLowerCase(Locale.ROOT));
            product.setFeaturesText(String.join("\n", input.features().stream()
                    .map(String::trim).filter(value -> !value.isEmpty()).toList()));
            product.setEligibility(input.eligibility().trim());
            product.setTermsAndConditions(input.termsAndConditions().trim());
            product.setImageData(input.imageData());
            product.setAnnualFee(input.annualFee());
            product.setSortOrder(input.sortOrder());
            product.setActive(input.active());
            product.setUpdatedBy(adminName.trim());
            saved.add(product);
        }
        return repository.saveAll(saved).stream().map(this::toView).toList();
    }

    private void validate(ProductInput input) {
        if (input == null || blank(input.productCode()) || !input.productCode().trim().matches("[A-Za-z0-9_-]{2,40}")
                || blank(input.name()) || input.name().trim().length() > 120
                || blank(input.description()) || input.description().trim().length() > 500
                || blank(input.theme()) || input.theme().trim().length() > 40
                || blank(input.eligibility()) || input.eligibility().length() > 10000
                || blank(input.termsAndConditions()) || input.termsAndConditions().length() > 50000
                || input.features() == null || input.features().size() > 12
                || input.features().stream().anyMatch(feature -> feature == null)
                || input.annualFee() == null || !Double.isFinite(input.annualFee())
                || input.annualFee() < 0 || input.sortOrder() == null || input.sortOrder() < 0
                || input.imageData() != null && !validImageData(input.imageData())) {
            throw new IllegalArgumentException("Check card name, description, features, eligibility, terms, fee, image and ordering.");
        }
    }

    private boolean validImageData(String imageData) {
        return imageData.length() <= 1_500_000
                && imageData.matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}");
    }

    private ProductView toView(CreditCardProduct product) {
        List<String> features = product.getFeaturesText() == null || product.getFeaturesText().isBlank()
                ? List.of()
                : product.getFeaturesText().lines().map(String::trim).filter(value -> !value.isEmpty()).toList();
        return new ProductView(product.getProductCode(), product.getName(), product.getDescription(),
                product.getTheme(), features, product.getEligibility(), product.getTermsAndConditions(),
                product.getImageData(), product.getAnnualFee(), product.getSortOrder(), product.isActive());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static ProductView defaultProduct(String code, String name, String description, String theme,
                                              int sortOrder, List<String> features) {
        return new ProductView(code, name, description, theme, features, DEFAULT_ELIGIBILITY,
                DEFAULT_TERMS, null, 0d, sortOrder, true);
    }

    public record ProductInput(String productCode, String name, String description, String theme,
                               List<String> features, String eligibility, String termsAndConditions,
                               String imageData, Double annualFee, Integer sortOrder, boolean active) {}

    public record ProductView(String productCode, String name, String description, String theme,
                              List<String> features, String eligibility, String termsAndConditions,
                              String imageData, Double annualFee, Integer sortOrder, boolean active) {}
}
