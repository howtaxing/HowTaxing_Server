package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultantInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConsultantInfoRepository extends JpaRepository<ConsultantInfo, Long> {

    @Query(value = "SELECT * FROM consultant_info c WHERE c.is_consulting_available = true ORDER BY consultant_id ASC", nativeQuery = true)
    List<ConsultantInfo> findAvailableConsultantList();
  
    Optional<ConsultantInfo> findByConsultantId(Long consultantId);
}
