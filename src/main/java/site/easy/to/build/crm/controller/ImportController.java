package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import site.easy.to.build.crm.service.importt.CustomerImportService;
import site.easy.to.build.crm.service.importt.CustomerImportService.ImportResult;
import site.easy.to.build.crm.service.importt.ImportService;

@Controller
@RequestMapping("/import")
public class ImportController {

    private final ImportService importService;
    CustomerImportService customerImportService;

    @Autowired
    public ImportController(ImportService importService, CustomerImportService customerImportService) {
        this.importService = importService;
        this.customerImportService= customerImportService;
    }
    
    @GetMapping("/")
    public String index(){
        return "import/import";
    }

    // @PostMapping("/employee")
    // public String importEmployees(@RequestParam("filePath") MultipartFile filePath) {
    //     try {
    //         importService.importEmployees(filePath);
    //     } catch (Exception e){
    //         return "error/500";
    //     }
    //     return "redirect:/";
    // }

    @PostMapping("/customer")
    public String importCustomers(@RequestParam("filePath") MultipartFile filePath, Model model) {
        try {
            ImportResult result = customerImportService.importCustomers(filePath);
            model.addAttribute("importResult", result);
            if (result.isSuccess()) {
                model.addAttribute("successMessage", result.getMessage());
            } else {
                model.addAttribute("errorMessage", result.getMessage());
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Une erreur inattendue s'est produite : " + e.getMessage());
        }
        return "import/import"; 
    }

}
