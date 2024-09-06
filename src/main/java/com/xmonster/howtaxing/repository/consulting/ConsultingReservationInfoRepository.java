package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultingReservationInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ConsultingReservationInfoRepository extends JpaRepository<ConsultingReservationInfo, Long> {

    Optional<ConsultingReservationInfo> findByConsultingReservationId(Long consultingReservationId);

    Long countByReservationDateAndReservationStartTime(LocalDate reservationDate, LocalTime reservationStartTime);

    List<ConsultingReservationInfo> findByUserId(Long userId);
}
