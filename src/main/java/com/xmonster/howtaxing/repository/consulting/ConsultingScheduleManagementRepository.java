package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultingScheduleId;
import com.xmonster.howtaxing.model.ConsultingScheduleManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ConsultingScheduleManagementRepository extends JpaRepository<ConsultingScheduleManagement, ConsultingScheduleId> {

     @Query(value = "SELECT * FROM consulting_schedule_management c WHERE (c.consultant_id = :consultantId AND c.reservation_date >= :today)", nativeQuery = true)
     List<ConsultingScheduleManagement> findByConsultantIdAfterToday(@Param("consultantId") Long consultantId, @Param("today") LocalDate today);

     ConsultingScheduleManagement findByConsultingScheduleId(ConsultingScheduleId consultingScheduleId);
}
