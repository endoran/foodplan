package com.endoran.foodplan.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class SharedMongoConfig {

    private static final Logger log = LoggerFactory.getLogger(SharedMongoConfig.class);

    @Value("${foodplan.shared.mongodb.uri:}")
    private String sharedMongoUri;

    @Value("${foodplan.instance.id:}")
    private String instanceId;

    @Bean
    public boolean globalRecipeBookEnabled() {
        boolean enabled = !sharedMongoUri.isBlank() && !instanceId.isBlank();
        if (enabled) {
            log.info("Global Recipe Book enabled (instance: {})", instanceId);
        } else {
            log.info("Global Recipe Book disabled (SHARED_MONGODB_URI or INSTANCE_ID not set)");
        }
        return enabled;
    }

    @Bean
    public SharedMongoHolder sharedMongoHolder() {
        if (sharedMongoUri.isBlank()) {
            return new SharedMongoHolder(null);
        }
        String dbName = extractDbName(sharedMongoUri);
        MongoClient client = MongoClients.create(sharedMongoUri);
        var factory = new SimpleMongoClientDatabaseFactory(client, dbName);
        MongoTemplate template = new MongoTemplate(factory);
        log.info("Connected to shared MongoDB: {}", dbName);
        return new SharedMongoHolder(template);
    }

    private String extractDbName(String uri) {
        String afterSlashes = uri.substring(uri.indexOf("//") + 2);
        String afterHost = afterSlashes.substring(afterSlashes.indexOf('/') + 1);
        int qIdx = afterHost.indexOf('?');
        return qIdx > 0 ? afterHost.substring(0, qIdx) : afterHost;
    }

    public static class SharedMongoHolder {
        private final MongoTemplate template;
        public SharedMongoHolder(MongoTemplate template) { this.template = template; }
        public MongoTemplate getTemplate() { return template; }
        public boolean isAvailable() { return template != null; }
    }
}
