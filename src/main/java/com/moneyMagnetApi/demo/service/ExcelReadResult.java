package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;

import java.util.List;

public record ExcelReadResult(
        List<Transaction> transactions,
        List<String> errors
) {}

