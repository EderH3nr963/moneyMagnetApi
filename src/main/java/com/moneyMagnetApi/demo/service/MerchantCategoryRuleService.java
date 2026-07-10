package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.category.MerchantCategoryRule;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.category.request.CreateMerchantCategoryRuleRequest;
import com.moneyMagnetApi.demo.dto.category.request.UpdateMerchantCategoryRuleRequest;
import com.moneyMagnetApi.demo.dto.category.response.MerchantCategoryRuleResponse;
import com.moneyMagnetApi.demo.repository.MerchantCategoryRuleRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import com.moneyMagnetApi.demo.utils.StringNormalize;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantCategoryRuleService {

    private final MerchantCategoryRuleRepository merchantCategoryRuleRepository;
    private final TransactionRepository transactionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final Cache<UUID, List<MerchantCategoryRuleResponse>> merchantCategoryRulesByUserCache;
    private final Cache<UUID, Map<String, Category>> activeMerchantCategoryRulesByUserCache;

    @Transactional
    public MerchantCategoryRuleResponse create(UUID userId, CreateMerchantCategoryRuleRequest request) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
        Category category = authorizationService.validateCategory(userId, request.categoryId());

        String merchant = request.merchant().trim();
        String normalizedMerchant = normalizeMerchant(merchant);

        if (merchantCategoryRuleRepository.existsByUsuarioIdAndNormalizedMerchant(userId, normalizedMerchant)) {
            throw new ValidationException("Ja existe uma regra para esse merchant");
        }

        MerchantCategoryRule rule = MerchantCategoryRule.builder()
                .merchant(merchant)
                .normalizedMerchant(normalizedMerchant)
                .category(category)
                .usuario(usuario)
                .build();

        MerchantCategoryRule savedRule = merchantCategoryRuleRepository.save(rule);
        applyRuleToExistingTransactions(userId, savedRule);
        invalidateUserRuleCaches(userId);

        return MerchantCategoryRuleResponse.fromRule(savedRule);
    }

    @Transactional(readOnly = true)
    public List<MerchantCategoryRuleResponse> findAll(UUID userId) {
        return merchantCategoryRulesByUserCache.get(userId, this::loadRuleResponses);
    }

    private List<MerchantCategoryRuleResponse> loadRuleResponses(UUID userId) {
        return merchantCategoryRuleRepository.findAllByUsuarioIdOrderByMerchantAsc(userId)
                .stream()
                .map(MerchantCategoryRuleResponse::fromRule)
                .toList();
    }

    @Transactional
    public MerchantCategoryRuleResponse update(
            UUID userId,
            UUID ruleId,
            UpdateMerchantCategoryRuleRequest request
    ) {
        MerchantCategoryRule rule = merchantCategoryRuleRepository.findByIdAndUsuarioId(ruleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Regra de merchant nao encontrada"));
        Category category = authorizationService.validateCategory(userId, request.categoryId());

        rule.setCategory(category);
        if (request.active() != null) {
            rule.setActive(request.active());
        }

        MerchantCategoryRule savedRule = merchantCategoryRuleRepository.save(rule);
        if (savedRule.isActive()) {
            applyRuleToExistingTransactions(userId, savedRule);
        }
        invalidateUserRuleCaches(userId);

        return MerchantCategoryRuleResponse.fromRule(savedRule);
    }

    @Transactional
    public void delete(UUID userId, UUID ruleId) {
        MerchantCategoryRule rule = merchantCategoryRuleRepository.findByIdAndUsuarioId(ruleId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Regra de merchant nao encontrada"));

        merchantCategoryRuleRepository.delete(rule);
        invalidateUserRuleCaches(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Category> getActiveRulesByMerchant(UUID userId) {
        return activeMerchantCategoryRulesByUserCache.get(userId, this::loadActiveRulesByMerchant);
    }

    private Map<String, Category> loadActiveRulesByMerchant(UUID userId) {
        return merchantCategoryRuleRepository.findAllByUsuarioIdAndActiveTrue(userId)
                .stream()
                .collect(Collectors.toMap(
                        MerchantCategoryRule::getNormalizedMerchant,
                        MerchantCategoryRule::getCategory
                ));
    }

    public Category resolveCategoryForMerchant(Map<String, Category> rulesByMerchant, String merchant) {
        if (!StringUtils.hasText(merchant)) {
            return null;
        }

        return rulesByMerchant.get(normalizeMerchant(merchant));
    }

    private void applyRuleToExistingTransactions(UUID userId, MerchantCategoryRule rule) {
        List<Transaction> transactions = transactionRepository.findAllByAccountItemUsuarioIdAndMerchantIsNotNull(userId);

        transactions.stream()
                .filter(transaction ->
                        rule.getNormalizedMerchant().equals(normalizeMerchant(transaction.getMerchant())))
                .forEach(transaction -> transaction.setCategory(rule.getCategory()));
    }

    private String normalizeMerchant(String merchant) {
        if (!StringUtils.hasText(merchant)) {
            throw new ValidationException("Merchant invalido");
        }

        return StringNormalize.normalize(merchant);
    }

    private void invalidateUserRuleCaches(UUID userId) {
        merchantCategoryRulesByUserCache.invalidate(userId);
        activeMerchantCategoryRulesByUserCache.invalidate(userId);
    }
}
