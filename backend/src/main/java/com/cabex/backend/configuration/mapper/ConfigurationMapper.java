package com.cabex.backend.configuration.mapper;

import com.cabex.backend.configuration.dto.ConfigurationResponse;
import com.cabex.backend.configuration.entity.Configuration;

public class ConfigurationMapper {

    private ConfigurationMapper() {
    }

    public static ConfigurationResponse toResponse(Configuration config) {
        if (config == null) {
            return null;
        }
        return new ConfigurationResponse(
                config.getId(),
                config.getCle(),
                config.getValeur(),
                config.getTypeValeur(),
                config.getDescription(),
                config.getModifiable()
        );
    }
}
