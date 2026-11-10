package com.temple.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    ApplicationRunner dropUserEmailUniqueIndex(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                if (!mongoTemplate.collectionExists("users")) return;
                IndexOperations indexOps = mongoTemplate.indexOps("users");
                for (var idx : indexOps.getIndexInfo()) {
                    // Allow multiple accounts to share the same email: remove unique index on email field
                    if (idx.isUnique() && idx.getIndexFields().stream().anyMatch(f -> "email".equals(f.getKey()))) {
                        try {
                            indexOps.dropIndex(idx.getName());
                            log.info("Dropped unique users.email index '{}' to allow duplicate emails across accounts.", idx.getName());
                        } catch (Exception e) {
                            log.warn("Could not drop users.email index '{}': {}", idx.getName(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not check/drop users.email unique index: {}", e.getMessage());
            }
        };
    }
}
