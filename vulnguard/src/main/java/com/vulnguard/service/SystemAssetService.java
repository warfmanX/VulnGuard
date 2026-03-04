package com.vulnguard.service;

import com.vulnguard.domain.ScanReport;
import com.vulnguard.domain.SystemAsset;
import com.vulnguard.domain.Vulnerability;
import com.vulnguard.dto.SystemAssetDto;
import com.vulnguard.repository.ScanReportRepository;
import com.vulnguard.repository.SystemAssetRepository;
import com.vulnguard.repository.VulnerabilityRepository;
import com.vulnguard.web.api.error.NotFoundException;
import com.vulnguard.mapper.SystemAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet; // <--- ИЗМЕНЕНИЕ 1: HashSet вместо ArrayList
import java.util.List;
import java.util.Optional;
import java.util.Set;     // <--- ИЗМЕНЕНИЕ 2: Set вместо List
import java.util.stream.Collectors;

@Service
@Transactional
public class SystemAssetService {

    private final SystemAssetRepository assetRepository;
    private final ScanReportRepository scanReportRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    public SystemAssetService(SystemAssetRepository assetRepository,
                              ScanReportRepository scanReportRepository,
                              VulnerabilityRepository vulnerabilityRepository) {
        this.assetRepository = assetRepository;
        this.scanReportRepository = scanReportRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
    }

    // === ИСПРАВЛЕННЫЙ МЕТОД СКАНИРОВАНИЯ ===
    public void runScan(Long assetId) {
        SystemAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + assetId));

        List<Vulnerability> allVulns = vulnerabilityRepository.findAll();
        ScanReport report = new ScanReport();
        report.setAsset(asset);
        report.setTimestamp(Instant.now());

        if (allVulns.isEmpty()) {
            // nothing to report, mark healthy
            report.setVulnerabilities(new HashSet<>());
            report.setStatus(ScanReport.Status.COMPLETED);
        } else {
            // pick 1-3 random vulnerabilities to simulate a scan result
            java.util.Collections.shuffle(allVulns);
            int count = 1 + (int) (Math.random() * 3); // 1,2 or 3
            count = Math.min(count, allVulns.size());
            Set<Vulnerability> picked = new HashSet<>(allVulns.subList(0, count));
            report.setVulnerabilities(picked);
            // by definition a scan that returns vulnerabilities is vulnerable
            report.setStatus(ScanReport.Status.VULNERABLE);
        }
        scanReportRepository.save(report);
    }

    // === Остальные методы ===

    public List<SystemAssetDto> findAll() {
        return assetRepository.findAll()
                .stream()
                .map(SystemAssetMapper::toDto)
                .collect(Collectors.toList());
    }

    public SystemAssetDto findById(Long id) {
        SystemAsset asset = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + id));
        return SystemAssetMapper.toDto(asset);
    }

    public SystemAssetDto create(SystemAssetDto dto) {
        SystemAsset asset = SystemAssetMapper.toEntity(dto);
        asset.setId(null);
        return SystemAssetMapper.toDto(assetRepository.save(asset));
    }

    public SystemAssetDto update(Long id, SystemAssetDto dto) {
        SystemAsset existing = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + id));

        existing.setHostname(dto.getHostname());
        existing.setIpAddress(dto.getIpAddress());
        existing.setOs(dto.getOs());
        existing.setImportanceLevel(dto.getImportanceLevel());

        return SystemAssetMapper.toDto(assetRepository.save(existing));
    }

    public void delete(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new NotFoundException("SystemAsset not found: " + id);
        }
        assetRepository.deleteById(id);
    }
}