package com.vulnguard.service;

import com.vulnguard.domain.ScanReport;
import com.vulnguard.domain.SystemAsset;
import com.vulnguard.domain.Vulnerability;
import com.vulnguard.dto.ScanReportDto;
import com.vulnguard.repository.ScanReportRepository;
import com.vulnguard.repository.SystemAssetRepository;
import com.vulnguard.repository.VulnerabilityRepository;
import com.vulnguard.web.api.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScanReportService {

    private final ScanReportRepository scanReportRepository;
    private final SystemAssetRepository systemAssetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    public ScanReportService(ScanReportRepository scanReportRepository,
                             SystemAssetRepository systemAssetRepository,
                             VulnerabilityRepository vulnerabilityRepository) {
        this.scanReportRepository = scanReportRepository;
        this.systemAssetRepository = systemAssetRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
    }

    public List<ScanReportDto> findAll() {
        return scanReportRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ScanReportDto findById(Long id) {
        ScanReport report = scanReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ScanReport not found: " + id));
        return toDto(report);
    }

    public ScanReportDto create(ScanReportDto dto) {
        ScanReport report = toEntity(dto);
        report.setId(null);
        return toDto(scanReportRepository.save(report));
    }

    public ScanReportDto update(Long id, ScanReportDto dto) {
        ScanReport existing = scanReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ScanReport not found: " + id));

        existing.setStatus(dto.getStatus());
        existing.setTimestamp(dto.getTimestamp());

        SystemAsset asset = systemAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + dto.getAssetId()));
        existing.setAsset(asset);

        List<Vulnerability> vulns = vulnerabilityRepository.findAllById(dto.getVulnerabilityIds());
        existing.setVulnerabilities(new HashSet<>(vulns));

        return toDto(scanReportRepository.save(existing));
    }

    public void delete(Long id) {
        if (!scanReportRepository.existsById(id)) {
            throw new NotFoundException("ScanReport not found: " + id);
        }
        scanReportRepository.deleteById(id);
    }

    private ScanReportDto toDto(ScanReport entity) {
        ScanReportDto dto = new ScanReportDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setTimestamp(entity.getTimestamp());
        dto.setAssetId(entity.getAsset().getId());
        dto.setVulnerabilityIds(
                entity.getVulnerabilities()
                        .stream()
                        .map(Vulnerability::getId)
                        .collect(Collectors.toList())
        );
        return dto;
    }

    private ScanReport toEntity(ScanReportDto dto) {
        ScanReport entity = new ScanReport();
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setTimestamp(dto.getTimestamp());

        SystemAsset asset = systemAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new NotFoundException("SystemAsset not found: " + dto.getAssetId()));
        entity.setAsset(asset);

        List<Vulnerability> vulns = vulnerabilityRepository.findAllById(dto.getVulnerabilityIds());
        entity.setVulnerabilities(new HashSet<>(vulns));

        return entity;
    }
}

