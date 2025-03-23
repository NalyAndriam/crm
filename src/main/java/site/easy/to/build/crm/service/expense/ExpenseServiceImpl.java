package site.easy.to.build.crm.service.expense;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.repository.ExpenseRepository;

@Service
public class ExpenseServiceImpl implements ExpenseService{

    private final ExpenseRepository expenseRepository;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public Expense save(Expense expense) {
        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    @Override
    public List<Expense> findByCustomerId(Integer customerId){
        return expenseRepository.findByCustomerCustomerId(customerId);
    }

    @Override
    public BigDecimal sumAmountByCustomerId(@Param("customerId") Integer customerId){
        return expenseRepository.sumAmountByCustomerId(customerId);
    }
    
    
}
