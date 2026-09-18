package com.interior.platform.portfolio.domain;

import java.util.Set;

public enum SectionType {
    HERO(true, true),
    ABOUT(true, true),
    SERVICES(true, true),
    FEATURED_PROJECTS(false, false),
    PROJECT_GRID(false, false),
    BEFORE_AFTER(false, false),
    BEFORE_AI_REALITY(false, false),
    DESIGN_PROCESS(false, false),
    TESTIMONIALS(false, false),
    TEAM(false, false),
    AWARDS(false, false),
    PRESS(false, false),
    SERVICE_AREAS(true, false),
    FAQ(false, false),
    CONTACT(true, true),
    CTA(true, false),
    VIDEO(false, false),
    CUSTOM_NOTE(false, false);

    private final boolean singleton;
    private final boolean requiredForReady;

    SectionType(boolean singleton, boolean requiredForReady) {
        this.singleton = singleton;
        this.requiredForReady = requiredForReady;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public boolean isRequiredForReady() {
        return requiredForReady;
    }

    public static final Set<SectionType> REQUIRED_SECTION_TYPES = Set.of(
            HERO, ABOUT, SERVICES, CONTACT
    );
}
