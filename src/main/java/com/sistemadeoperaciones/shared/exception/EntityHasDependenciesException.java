package com.sistemadeoperaciones.shared.exception;

import java.util.Map;

public class EntityHasDependenciesException extends BusinessException {

    private final Map<String, Long> dependencies;

    public EntityHasDependenciesException(String message, Map<String, Long> dependencies) {
        super(message);
        this.dependencies = dependencies;
    }

    public Map<String, Long> getDependencies() {
        return dependencies;
    }
}
