package com.vulnguard.web.view;

import com.vulnguard.domain.SystemAsset;
import com.vulnguard.dto.SystemAssetDto;
import com.vulnguard.service.SystemAssetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // <--- ВОТ ЭТОГО НЕ ХВАТАЛО
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final SystemAssetService systemAssetService;

    public DashboardController(SystemAssetService systemAssetService) {
        this.systemAssetService = systemAssetService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("assets", systemAssetService.findAll());
        return "dashboard";
    }

    @PostMapping("/add-asset")
    public String addAsset(@RequestParam String hostname,
                           @RequestParam String ipAddress,
                           @RequestParam String os,
                           @RequestParam String importanceLevel) {
        
        SystemAssetDto newAsset = new SystemAssetDto();
        newAsset.setHostname(hostname);
        newAsset.setIpAddress(ipAddress);
        newAsset.setOs(os);
        
        // Превращаем строку "HIGH" в настоящий Enum
        newAsset.setImportanceLevel(SystemAsset.ImportanceLevel.valueOf(importanceLevel));

        systemAssetService.create(newAsset);

        return "redirect:/dashboard";
    }

    @PostMapping("/scan/{id}")
    public String scanAsset(@PathVariable Long id) {
        systemAssetService.runScan(id);
        
        return "redirect:/dashboard";
    }
}