package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import site.easy.to.build.crm.service.reset.ResetService;

@Controller
@RequestMapping("/reset")
public class ResetController {

    private final ResetService resetService;

    @Autowired
    public ResetController(ResetService resetService) {
        this.resetService = resetService;
    }
    
    @GetMapping("/")
    public String index(){
        return "reset/reset";
    }

    @GetMapping("/all")
    public String resetDatabase(){
        try {
            resetService.resetDatabase();
        } catch (Exception e){
            return "error/500";
        }
        return "redirect:/";
    }
}
