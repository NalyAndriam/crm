package site.easy.to.build.crm.service.budget;

import java.util.List;

import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Budget;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.repository.BudgetRepository;

@Service
public class BudgetServiceImpl implements BudgetService{

    private final BudgetRepository budgetRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    public Budget save(Budget budget) {
        return budgetRepository.save(budget);
    }

    @Override
    public List<Budget> findAll() {
        return budgetRepository.findAll();
    }
    
    
}
