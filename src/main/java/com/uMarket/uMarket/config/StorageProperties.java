package com.uMarket.uMarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    public static final String PROVIDER_LOCAL = "local";
    public static final String PROVIDER_AZURE = "azure";

    private String provider = PROVIDER_LOCAL;

    private final Local local = new Local();
    private final Azure azure = new Azure();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Local getLocal() {
        return local;
    }

    public Azure getAzure() {
        return azure;
    }

    public static class Local {
        private String root = "./uploads";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class Azure {
        private String connectionString = "";
        private String containerName = "umarket-media";

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public boolean isConfigured() {
            return connectionString != null && !connectionString.isBlank();
        }
    }
}