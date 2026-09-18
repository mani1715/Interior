package com.interior.platform.portfolio.repository;

import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioSectionRecord;
import com.interior.platform.portfolio.domain.PortfolioVersionRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Optional<PortfolioRecord> findPortfolioByStudioId(UUID studioId);

    Optional<PortfolioRecord> findPortfolioById(UUID portfolioId);

    PortfolioRecord createPortfolio(PortfolioRecord portfolio);

    boolean updatePortfolio(PortfolioRecord portfolio, long expectedVersion);

    boolean incrementVersion(UUID portfolioId, long expectedVersion);

    boolean updateTemplateKey(UUID portfolioId, String templateKey, long expectedVersion);

    List<PortfolioSectionRecord> findSectionsByPortfolioId(UUID portfolioId);

    Optional<PortfolioSectionRecord> findSectionById(UUID sectionId, UUID portfolioId);

    void createSections(List<PortfolioSectionRecord> sections);

    void updateSection(PortfolioSectionRecord section);

    void reorderSections(UUID portfolioId, List<UUID> orderedSectionIds);

    void replaceAllSections(UUID portfolioId, UUID studioId, List<PortfolioSectionRecord> newSections);

    void createVersionSnapshot(PortfolioVersionRecord versionRecord);

    int getNextVersionNumber(UUID portfolioId);

    List<PortfolioVersionRecord> findVersionsByPortfolioId(UUID portfolioId, int limit);

    Optional<PortfolioVersionRecord> findVersionByNumber(UUID portfolioId, int versionNumber);

    void pruneOldVersions(UUID portfolioId, int retainMaxCount);
}
