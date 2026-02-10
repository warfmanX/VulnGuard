package com.vulnguard.service;

import com.vulnguard.domain.ScanReport;
import com.vulnguard.domain.SystemAsset;
import com.vulnguard.domain.Vulnerability;
import com.vulnguard.dto.SystemAssetDto;
import com.vulnguard.repository.ScanReportRepository;
import com.vulnguard.repository.SystemAssetRepository;
import com.vulnguard.repository.VulnerabilityRepository;
import com.vulnguard.web.api.error.NotFoundException;
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
        
        // ИСПОЛЬЗУЕМ SET, ЧТОБЫ СОВПАДАЛО С СУЩНОСТЬЮ SCANREPORT
        Set<Vulnerability> foundVulns = new HashSet<>();

        String osKeyword = "";
        if (asset.getOs() != null && !asset.getOs().isEmpty()) {
            osKeyword = asset.getOs().split(" ")[0].toLowerCase();
        }

        if (!osKeyword.isEmpty()) {
            for (Vulnerability v : allVulns) {
                boolean titleMatch = v.getTitle() != null && v.getTitle().toLowerCase().contains(osKeyword);
                boolean descMatch = v.getDescription() != null && v.getDescription().toLowerCase().contains(osKeyword);

                if (titleMatch || descMatch) {
                    foundVulns.add(v);
                }
            }
        }

        ScanReport report = new ScanReport();
        report.setAsset(asset);
        report.setTimestamp(Instant.now());
        report.setVulnerabilities(foundVulns); // Теперь здесь Set, и ошибки не будет
        report.setStatus(ScanReport.Status.COMPLETED);

        scanReportRepository.save(report);
    }

    // === Остальные методы ===

    public List<SystemAssetDto> findAll() {
        return assetRepository.findAll()
                .stream()
                .map(this::toDtoWithStatus)
                .collect(Collectors.toList());
    }

    public SystemAssetDto findById(Long id) {
        SystemAsset asset = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + id));
        return toDtoWithStatus(asset);
    }

    public SystemAssetDto create(SystemAssetDto dto) {
        SystemAsset asset = toEntity(dto);
        asset.setId(null);
        return toDtoWithStatus(assetRepository.save(asset));
    }

    public SystemAssetDto update(Long id, SystemAssetDto dto) {
        SystemAsset existing = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + id));

        existing.setHostname(dto.getHostname());
        existing.setIpAddress(dto.getIpAddress());
        existing.setOs(dto.getOs());
        existing.setImportanceLevel(dto.getImportanceLevel());

        return toDtoWithStatus(assetRepository.save(existing));
    }

    public void delete(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new NotFoundException("SystemAsset not found: " + id);
        }
        assetRepository.deleteById(id);
    }

    private SystemAssetDto toDtoWithStatus(SystemAsset asset) {
        SystemAssetDto dto = toDto(asset);

        Optional<ScanReport> latestReport = asset.getScanReports()
                .stream()
                .max(Comparator.comparing(ScanReport::getTimestamp, Comparator.nullsLast(Instant::compareTo)));

        String status = latestReport
                .map(report -> {
                    if (report.getStatus() != ScanReport.Status.COMPLETED) {
                        return "SCAN_" + report.getStatus().name();
                    }
                    boolean hasHighSeverity = report.getVulnerabilities()
                            .stream()
                            .anyMatch(v -> v.getSeverityScore() != null && v.getSeverityScore().doubleValue() >= 7.0);
                    if (hasHighSeverity) {
                        return "AT_RISK";
                    }
                    return "HEALTHY";
                })
                .orElse("NOT_SCANNED");

        dto.setCurrentSecurityStatus(status);
        return dto;
    }

    private SystemAssetDto toDto(SystemAsset asset) {
        SystemAssetDto dto = new SystemAssetDto();
        dto.setId(asset.getId());
        dto.setHostname(asset.getHostname());
        dto.setIpAddress(asset.getIpAddress());
        dto.setOs(asset.getOs());
        dto.setImportanceLevel(asset.getImportanceLevel());
        return dto;
    }

    private SystemAsset toEntity(SystemAssetDto dto) {
        SystemAsset asset = new SystemAsset();
        asset.setId(dto.getId());
        asset.setHostname(dto.getHostname());
        asset.setIpAddress(dto.getIpAddress());
        asset.setOs(dto.getOs());
        asset.setImportanceLevel(dto.getImportanceLevel());
        return asset;
    }
}