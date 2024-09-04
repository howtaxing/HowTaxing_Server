package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultantInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultantInfoRepository extends JpaRepository<ConsultantInfo, Long> {

    Optional<ConsultantInfo> findByConsultantId(Long consultantId);
}
