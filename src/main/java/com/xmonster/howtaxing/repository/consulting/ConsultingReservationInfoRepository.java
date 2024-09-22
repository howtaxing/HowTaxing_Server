package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultingReservationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ConsultingReservationInfoRepository extends JpaRepository<ConsultingReservationInfo, Long> {

    Optional<ConsultingReservationInfo> findByConsultingReservationId(Long consultingReservationId);

    @Query(value = "SELECT COUNT(*) FROM consulting_reservation_info c WHERE (c.user_id = :userId AND c.reservation_date = :reservationDate AND c.is_canceled = false)", nativeQuery = true)
    Long countByUserIdAndReservationDate(Long userId, LocalDate reservationDate);

    @Query(value = "SELECT COUNT(*) FROM consulting_reservation_info c WHERE (c.reservation_date = :reservationDate AND c.reservation_start_time = :reservationStartTime AND c.is_canceled = false)", nativeQuery = true)
    Long countByReservationDateAndReservationStartTime(LocalDate reservationDate, LocalTime reservationStartTime);

    List<ConsultingReservationInfo> findByUserIdOrderByReservationDateDescReservationStartTimeDesc(Long userId);

    //List<ConsultingReservationInfo> findByReservationDate(LocalDate reservationDate);

    @Query(value = "SELECT * FROM consulting_reservation_info c WHERE (c.reservation_date = :reservationDate AND c.is_canceled = false)", nativeQuery = true)
    List<ConsultingReservationInfo> findByReservationDate(LocalDate reservationDate);
}
