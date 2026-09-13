package com.uMarket.uMarket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StorageConfigurationLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageConfigurationLogger.class);

    private final StorageProperties properties;

    public StorageConfigurationLogger(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String provider = properties.getProvider();
        log.info("Configuracion de almacenamiento: proveedor={}", provider);

        if (StorageProperties.PROVIDER_AZURE.equalsIgnoreCase(provider)) {
            if (!properties.getAzure().isConfigured()) {
                log.warn("Proveedor 'azure' activo pero AZURE_STORAGE_CONNECTION_STRING "
                        + "no esta definida. La subida de archivos fallara en tiempo de ejecucion.");
            } else {
                log.info("Azure Blob Storage configurado, contenedor={}", properties.getAzure().getContainerName());
            }
        } else if (StorageProperties.PROVIDER_LOCAL.equalsIgnoreCase(provider)) {
            log.info("Almacenamiento local: directorio raiz={}", properties.getLocal().getRoot());
        } else {
            log.warn("Proveedor de almacenamiento desconocido: '{}'. Valores validos: 'local', 'azure'.",
                    provider);
        }
    }
}