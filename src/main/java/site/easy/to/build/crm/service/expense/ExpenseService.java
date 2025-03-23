package site.easy.to.build.crm.service.expense;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.repository.query.Param;

import site.easy.to.build.crm.entity.Expense;


public interface ExpenseService {

    public Expense save(Expense Expense);

    public List<Expense> findAll();

    public List<Expense> findByCustomerId(Integer customerId);

    public BigDecimal sumAmountByCustomerId(Integer customerId);
    
}
