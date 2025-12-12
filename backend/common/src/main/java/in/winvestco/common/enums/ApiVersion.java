package in.winvestco.common.enums;

import in.winvestco.common.enums.ApiVersion;

/**
 * API Version Management
 * Centralized versioning strategy for all services
 */
public enum ApiVersion {
    V1("v1", "Initial API version", "2024-01-01"),
    V2("v2", "Enhanced API with new features", "2024-06-01");

    private final String version;
    private final String description;
    private final String releaseDate;

    ApiVersion(String version, String description, String releaseDate) {
        this.version = version;
        this.description = description;
        this.releaseDate = releaseDate;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getPath() {
        return "/api/" + version;
    }

    /**
     * Get the latest API version
     */
    public static ApiVersion getLatest() {
        return V2; // Update this when new versions are added
    }

    /**
     * Check if this version is deprecated
     */
    public boolean isDeprecated() {
        return this != getLatest();
    }
}
