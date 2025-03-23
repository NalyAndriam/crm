package site.easy.to.build.crm.service.rate;

import java.util.List;

import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Rate;
import site.easy.to.build.crm.repository.RateRepository;

@Service
public class RateServiceImpl implements RateService{

    private final RateRepository RateRepository;

    public RateServiceImpl(RateRepository RateRepository) {
        this.RateRepository = RateRepository;
    }

    @Override
    public Rate save(Rate Rate) {
        return RateRepository.save(Rate);
    }

    @Override
    public List<Rate> findAll() {
        return RateRepository.findAll();
    }

    @Override
    public Rate getLast() {
        return RateRepository.findTopByOrderByInsertedAtDesc();
    }
    
    
}
