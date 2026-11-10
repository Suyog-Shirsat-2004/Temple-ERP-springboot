package com.temple.erp.repository;

import com.temple.erp.model.MailSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MailSettingRepository extends MongoRepository<MailSetting, String> {
}
