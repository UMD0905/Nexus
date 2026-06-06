package com.nexus.service;

import com.nexus.model.Transaction;
import com.nexus.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class TransactionService {
    private final TransactionRepository repo;
    public TransactionService(TransactionRepository repo) { this.repo = repo; }
    public List<Transaction> getAll()                          { return repo.findAll(); }
    public List<Transaction> getByMonth(int year, int month)   { return repo.findByMonth(year, month); }
    public Transaction add(Transaction t)                      { return repo.save(t); }
    public Transaction update(Transaction t)                   { return repo.update(t); }
    public void delete(long id)                                { repo.delete(id); }

    /** Returns totals map: { "incomeUzs", "expenseUzs", "incomeUsd", "expenseUsd" } */
    public Map<String,BigDecimal> getTotals(List<Transaction> txns) {
        BigDecimal incomeUzs = BigDecimal.ZERO, expenseUzs = BigDecimal.ZERO;
        BigDecimal incomeUsd = BigDecimal.ZERO, expenseUsd = BigDecimal.ZERO;
        for (Transaction t : txns) {
            boolean isIncome = "INCOME".equals(t.getType());
            boolean isUzs    = "UZS".equals(t.getCurrency());
            if (isIncome  && isUzs)  incomeUzs  = incomeUzs.add(t.getAmount());
            if (!isIncome && isUzs)  expenseUzs = expenseUzs.add(t.getAmount());
            if (isIncome  && !isUzs) incomeUsd  = incomeUsd.add(t.getAmount());
            if (!isIncome && !isUzs) expenseUsd = expenseUsd.add(t.getAmount());
        }
        Map<String,BigDecimal> m = new LinkedHashMap<>();
        m.put("incomeUzs",  incomeUzs);
        m.put("expenseUzs", expenseUzs);
        m.put("incomeUsd",  incomeUsd);
        m.put("expenseUsd", expenseUsd);
        return m;
    }
}
