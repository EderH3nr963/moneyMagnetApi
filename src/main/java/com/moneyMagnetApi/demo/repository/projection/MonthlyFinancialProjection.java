package com.moneyMagnetApi.demo.repository.projection;

import java.math.BigDecimal;

public interface MonthlyFinancialProjection {
    
    Integer getYear();
    
    Integer getMonth();
    
    BigDecimal getIncome();
    
    BigDecimal getExpenses();
}