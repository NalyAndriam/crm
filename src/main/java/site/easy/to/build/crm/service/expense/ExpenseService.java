package site.easy.to.build.crm.service.expense;

import java.util.List;

import site.easy.to.build.crm.entity.Expense;


public interface ExpenseService {

    public Expense save(Expense Expense);

    public List<Expense> findAll();
    
}
