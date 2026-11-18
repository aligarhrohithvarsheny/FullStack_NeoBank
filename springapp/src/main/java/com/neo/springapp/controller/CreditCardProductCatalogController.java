package com.neo.springapp.controller;

import com.neo.springapp.service.CreditCardProductCatalogService;
import com.neo.springapp.service.CreditCardProductCatalogService.ProductInput;
import com.neo.springapp.service.CreditCardProductCatalogService.ProductView;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-card-products")
public class CreditCardProductCatalogController {
    private final CreditCardProductCatalogService service;

    public CreditCardProductCatalogController(CreditCardProductCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductView> getActiveProducts() {
        return service.getPublicProducts();
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminProducts(Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.getAdminProducts());
    }

    @PutMapping("/admin")
    public ResponseEntity<?> saveProducts(@RequestBody(required = false) List<ProductInput> products,
                                          Authentication authentication) {
        SessionPrincipal principal = adminPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(service.saveAll(products, principal.accountNumber()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return adminPrincipal(authentication) != null;
    }

    private SessionPrincipal adminPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || !"ADMIN".equals(principal.scope())) return null;
        return principal;
    }
}
