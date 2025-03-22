package site.easy.to.build.crm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.Budget;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    public Budget findByBudgetId(int budgetId);

    List<Budget> findByCustomerCustomerId(Integer customerId);

}
