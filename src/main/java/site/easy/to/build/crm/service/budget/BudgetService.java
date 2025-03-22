package site.easy.to.build.crm.service.budget;

import java.util.List;

import site.easy.to.build.crm.entity.Budget;


public interface BudgetService {

    public Budget save(Budget budget);

    public List<Budget> findAll();
    
}
