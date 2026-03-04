package com.vulnguard.service;

import com.vulnguard.domain.ScanReport;
import com.vulnguard.domain.SystemAsset;
import com.vulnguard.domain.Vulnerability;
import com.vulnguard.repository.ScanReportRepository;
import com.vulnguard.repository.SystemAssetRepository;
import com.vulnguard.repository.VulnerabilityRepository;
import com.vulnguard.web.api.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class SystemAssetServiceTest {

    private ScanReportRepository scanRepo;
    private SystemAssetRepository assetRepo;
    private VulnerabilityRepository vulnRepo;
    private SystemAssetService service;

    @BeforeEach
    void setup() {
        scanRepo = Mockito.mock(ScanReportRepository.class);
        assetRepo = Mockito.mock(SystemAssetRepository.class);
        vulnRepo = Mockito.mock(VulnerabilityRepository.class);
        service = new SystemAssetService(assetRepo, scanRepo, vulnRepo);
    }

    @Test
    void runScan_should_throw_if_asset_not_found() {
        when(assetRepo.findById(123L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.runScan(123L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SystemAsset not found");
    }

    @Test
    void runScan_with_no_vulnerabilities_saves_empty_completed_report() {
        SystemAsset asset = new SystemAsset();
        asset.setId(5L);
        when(assetRepo.findById(5L)).thenReturn(Optional.of(asset));
        when(vulnRepo.findAll()).thenReturn(Collections.emptyList());

        service.runScan(5L);

        ArgumentCaptor<ScanReport> captor = ArgumentCaptor.forClass(ScanReport.class);
        Mockito.verify(scanRepo).save(captor.capture());
        ScanReport saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ScanReport.Status.COMPLETED);
        assertThat(saved.getVulnerabilities()).isEmpty();
    }

    @Test
    void runScan_with_vulnerabilities_saves_vulnerable_report() {
        SystemAsset asset = new SystemAsset();
        asset.setId(10L);
        when(assetRepo.findById(10L)).thenReturn(Optional.of(asset));
        Vulnerability v1 = new Vulnerability();
        Vulnerability v2 = new Vulnerability();
        // use mutable list so shuffle() inside service doesn't fail
        when(vulnRepo.findAll()).thenReturn(new java.util.ArrayList<>(List.of(v1, v2)));

        service.runScan(10L);

        ArgumentCaptor<ScanReport> captor = ArgumentCaptor.forClass(ScanReport.class);
        Mockito.verify(scanRepo).save(captor.capture());
        ScanReport saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ScanReport.Status.VULNERABLE);
        assertThat(saved.getVulnerabilities()).isNotEmpty();
        assertThat(saved.getVulnerabilities().size()).isBetween(1, 3);
    }
}
