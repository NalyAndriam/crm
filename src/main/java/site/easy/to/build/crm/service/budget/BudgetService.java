package site.easy.to.build.crm.service.budget;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.repository.query.Param;

import site.easy.to.build.crm.entity.Budget;


public interface BudgetService {

    public Budget save(Budget budget);

    public List<Budget> findAll();

    public List<Budget> findByCustomerId(Integer customerId);

    public BigDecimal sumAmountByCustomerId( Integer customerId);

    public BigDecimal getSumAmount();
    
}
