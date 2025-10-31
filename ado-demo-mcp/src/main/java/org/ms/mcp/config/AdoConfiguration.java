package org.ms.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ado")
public class AdoConfiguration {
    
    private String organization;
    private String project;
    private String patToken;
    private Api api = new Api();
    
    @Data
    public static class Api {
        private String baseUrl = "/_apis";
        private Versions versions = new Versions();
        private Endpoints endpoints = new Endpoints();
        
        @Data
        public static class Versions {
            private String wiql = "7.1-preview.2";
            private String workItems = "7.1-preview.3";
            private String git = "7.1-preview.1";
            private String project = "7.1-preview.4";
        }
        
        @Data
        public static class Endpoints {
            private String wiql = "/wit/wiql";
            private String workItems = "/wit/workitems";
            private String projects = "/projects";
            private String repositories = "/git/repositories";
        }
    }
    
    // Helper methods para construir URLs completas
    public String buildWiqlUrl() {
        return organization + api.baseUrl + api.endpoints.wiql + "?api-version=" + api.versions.wiql;
    }
    
    public String buildWorkItemsUrl() {
        return organization + api.baseUrl + api.endpoints.workItems;
    }
    
    public String buildWorkItemByIdUrl(Long id) {
        return buildWorkItemsUrl() + "/" + id + "?api-version=" + api.versions.workItems;
    }
    
    public String buildWorkItemWithRelationsUrl(Long id) {
        return buildWorkItemsUrl() + "/" + id + "?$expand=relations&api-version=" + api.versions.workItems;
    }
    
    public String buildRepositoriesUrl() {
        return organization + api.baseUrl + api.endpoints.repositories + "?api-version=" + api.versions.git;
    }
}