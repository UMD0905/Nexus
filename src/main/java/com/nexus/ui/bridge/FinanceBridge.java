package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.Transaction;
import com.nexus.service.SettingsService;
import com.nexus.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge methods for finance (income/expense tracking) operations.
 * Injected as {@code window.nexusBridgeFinance} by {@code MainWindow.tryFinish()}.
 */
public class FinanceBridge {

    private static final Logger log = LoggerFactory.getLogger(FinanceBridge.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public FinanceBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** Returns all transactions as a JSON array. */
    public String getTransactions() {
        try {
            List<Transaction> txns = service().getAll();
            return toJson(txns.stream().map(this::toDto).toList());
        } catch (Exception e) { return error(e); }
    }

    /** Returns transactions for a given year/month as a JSON array. */
    public String getTransactionsByMonth(int year, int month) {
        try {
            List<Transaction> txns = service().getByMonth(year, month);
            return toJson(txns.stream().map(this::toDto).toList());
        } catch (Exception e) { return error(e); }
    }

    // ── Mutation methods ──────────────────────────────────────────────────────

    /**
     * Parses the incoming JSON, saves the transaction, and returns it (with generated id).
     * Input fields: type, amount, currency, category, description, txnDate (ISO "YYYY-MM-DD").
     */
    public String addTransaction(String txnJson) {
        try {
            JsonNode node = json.readTree(txnJson);
            Transaction t = Transaction.builder()
                .type(node.path("type").asText())
                .amount(new BigDecimal(node.path("amount").asText("0")))
                .currency(node.has("currency") ? node.path("currency").asText("UZS") : "UZS")
                .category(nullableText(node, "category"))
                .description(nullableText(node, "description"))
                .txnDate(LocalDate.parse(node.path("txnDate").asText()))
                .createdAt(LocalDateTime.now())
                .build();
            Transaction saved = service().add(t);
            return toJson(toDto(saved));
        } catch (Exception e) { return error(e); }
    }

    /**
     * Updates an existing transaction. Input JSON must include {@code id} plus
     * any fields to change (type, amount, currency, category, description, txnDate).
     */
    public String updateTransaction(String txnJson) {
        try {
            JsonNode node = json.readTree(txnJson);
            long id = node.path("id").asLong();
            // Load existing, then overlay changed fields
            List<Transaction> all = service().getAll();
            Transaction existing = all.stream().filter(t -> t.getId() != null && t.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
            if (node.has("type"))        existing.setType(node.path("type").asText());
            if (node.has("amount"))      existing.setAmount(new BigDecimal(node.path("amount").asText()));
            if (node.has("currency"))    existing.setCurrency(node.path("currency").asText());
            if (node.has("category"))    existing.setCategory(nullableText(node, "category"));
            if (node.has("description")) existing.setDescription(nullableText(node, "description"));
            if (node.has("txnDate"))     existing.setTxnDate(LocalDate.parse(node.path("txnDate").asText()));
            Transaction saved = service().update(existing);
            return toJson(toDto(saved));
        } catch (Exception e) { return error(e); }
    }

    /** Deletes a transaction by id. */
    public void deleteTransaction(long id) {
        try { service().delete(id); }
        catch (Exception e) { log.error("deleteTransaction failed", e); }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /**
     * Returns JSON with totals for all time AND for the current month.
     * Any manually-set overrides (stored in APP_SETTINGS) take precedence.
     * Shape: { all: { incomeUzs, expenseUzs, incomeUsd, expenseUsd, balanceUzs, balanceUsd },
     *          month: { same fields } }
     */
    public String getStats() {
        try {
            TransactionService svc = service();
            SettingsService ss = ctx.getSettingsService();

            List<Transaction> all    = svc.getAll();
            LocalDate today = LocalDate.now();
            List<Transaction> month  = svc.getByMonth(today.getYear(), today.getMonthValue());

            Map<String, Object> allBlock   = new LinkedHashMap<>(totalsBlock(svc.getTotals(all)));
            Map<String, Object> monthBlock = new LinkedHashMap<>(totalsBlock(svc.getTotals(month)));

            // Apply manual overrides if set
            applyOverride(ss, allBlock,   "balanceUzs",  "all.balance_uzs");
            applyOverride(ss, allBlock,   "balanceUsd",  "all.balance_usd");
            applyOverride(ss, monthBlock, "incomeUzs",   "month.income_uzs");
            applyOverride(ss, monthBlock, "incomeUsd",   "month.income_usd");
            applyOverride(ss, monthBlock, "expenseUzs",  "month.expense_uzs");
            applyOverride(ss, monthBlock, "expenseUsd",  "month.expense_usd");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("all",   allBlock);
            result.put("month", monthBlock);
            return toJson(result);
        } catch (Exception e) { return error(e); }
    }

    // ── Manual overrides ──────────────────────────────────────────────────────

    private static final String OVERRIDE_PREFIX = "finance.override.";
    private static final java.util.List<String> OVERRIDE_KEYS = java.util.List.of(
        "all.balance_uzs", "all.balance_usd",
        "month.income_uzs", "month.income_usd",
        "month.expense_uzs", "month.expense_usd"
    );

    /**
     * Returns JSON map of all currently active overrides.
     * Shape: { "all.balance_uzs": 1234.56, ... } (only keys that are set)
     */
    public String getOverrides() {
        try {
            SettingsService ss = ctx.getSettingsService();
            Map<String, Object> m = new LinkedHashMap<>();
            for (String key : OVERRIDE_KEYS) {
                ss.get(OVERRIDE_PREFIX + key).ifPresent(v -> {
                    try { m.put(key, new BigDecimal(v)); } catch (Exception ignored) {}
                });
            }
            return toJson(m);
        } catch (Exception e) { return error(e); }
    }

    /**
     * Stores a manual override for the given key.
     * @param key    one of: all.balance_uzs, all.balance_usd, month.income_uzs,
     *               month.income_usd, month.expense_uzs, month.expense_usd
     * @param amount decimal string, e.g. "12345.67"
     */
    public void setOverride(String key, String amount) {
        try {
            new BigDecimal(amount); // validate before storing
            ctx.getSettingsService().set(OVERRIDE_PREFIX + key, amount);
            log.debug("Finance override set: {}={}", key, amount);
        } catch (Exception e) { log.error("setOverride failed: {}", e.getMessage()); }
    }

    /** Removes the manual override for the given key (reverts to calculated value). */
    public void clearOverride(String key) {
        try {
            ctx.getSettingsService().delete(OVERRIDE_PREFIX + key);
            log.debug("Finance override cleared: {}", key);
        } catch (Exception e) { log.error("clearOverride failed: {}", e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TransactionService service() { return ctx.getTransactionService(); }

    /**
     * Returns the text value of a JSON field, or {@code null} if the field is
     * absent, explicitly {@code null}, or an empty/blank string.
     */
    private static String nullableText(JsonNode node, String field) {
        if (!node.has(field) || node.path(field).isNull()) return null;
        String v = node.path(field).asText("").trim();
        return v.isEmpty() ? null : v;
    }

    /** Converts a Transaction to a plain DTO map for JSON serialisation. */
    private Map<String, Object> toDto(Transaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          t.getId());
        m.put("type",        t.getType());
        m.put("amount",      t.getAmount());
        m.put("currency",    t.getCurrency());
        m.put("category",    t.getCategory());
        m.put("description", t.getDescription());
        m.put("txnDate",     t.getTxnDate() != null ? t.getTxnDate().toString() : null);
        m.put("createdAt",   t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return m;
    }

    /** Applies a stored override to a stats block field if the key is present. */
    private void applyOverride(SettingsService ss, Map<String, Object> block, String field, String overrideKey) {
        ss.get(OVERRIDE_PREFIX + overrideKey).ifPresent(v -> {
            try { block.put(field, new BigDecimal(v)); } catch (Exception ignored) {}
        });
    }

    /** Builds a stats block from a totals map, adding balance fields. */
    private Map<String, Object> totalsBlock(Map<String, BigDecimal> totals) {
        BigDecimal iUzs = totals.getOrDefault("incomeUzs",  BigDecimal.ZERO);
        BigDecimal eUzs = totals.getOrDefault("expenseUzs", BigDecimal.ZERO);
        BigDecimal iUsd = totals.getOrDefault("incomeUsd",  BigDecimal.ZERO);
        BigDecimal eUsd = totals.getOrDefault("expenseUsd", BigDecimal.ZERO);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("incomeUzs",  iUzs);
        m.put("expenseUzs", eUzs);
        m.put("incomeUsd",  iUsd);
        m.put("expenseUsd", eUsd);
        m.put("balanceUzs", iUzs.subtract(eUzs));
        m.put("balanceUsd", iUsd.subtract(eUsd));
        return m;
    }

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("FinanceBridge error: {}", e.getMessage(), e);
        try {
            return json.writeValueAsString(Map.of("error",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } catch (Exception ex) {
            return "{\"error\":\"unknown error\"}";
        }
    }
}
