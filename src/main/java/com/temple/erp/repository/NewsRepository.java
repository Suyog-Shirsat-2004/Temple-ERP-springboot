package com.temple.erp.repository;

import com.temple.erp.model.News;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NewsRepository extends MongoRepository<News, String> {
    List<News> findByStatusOrderByPublishedAtDesc(String status);
}
