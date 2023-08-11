package com.mohey.fcmpushservice.repository;

import com.mohey.fcmpushservice.document.FcmLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FcmLogRepository extends MongoRepository<FcmLog,String> {
}
