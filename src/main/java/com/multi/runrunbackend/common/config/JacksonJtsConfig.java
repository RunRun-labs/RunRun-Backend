package com.multi.runrunbackend.common.config;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : JacksonJtsConfig
 * @since : 2025. 12. 18. Thursday
 */

import com.fasterxml.jackson.databind.Module;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonJtsConfig {

    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }

    @Bean
    public Module jtsModuleAsModule(JtsModule jtsModule) {
        return jtsModule;
    }
}
