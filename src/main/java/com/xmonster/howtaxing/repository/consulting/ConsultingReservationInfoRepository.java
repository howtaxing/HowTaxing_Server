package com.xmonster.howtaxing.repository.consulting;

import com.xmonster.howtaxing.model.ConsultingReservationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ConsultingReservationInfoRepository extends JpaRepository<ConsultingReservationInfo, Long> {

    Optional<ConsultingReservationInfo> findByConsultingReservationId(Long consultingReservationId);

    @Query(value = "SELECT COUNT(*) " +
                    "FROM consulting_reservation_info c " +
                    "WHERE (c.user_id = :userId " +
                        "AND c.reservation_date = :reservationDate " +
                        "AND c.consulting_status != 'PAYMENT_READY' " +
                        "AND c.is_canceled = false)", nativeQuery = true)
    Long countByUserIdAndReservationDate(@Param("userId") Long userId,
                                         @Param("reservationDate") LocalDate reservationDate);

    @Query(value = "SELECT COUNT(*) " +
                    "FROM consulting_reservation_info c " +
                    "WHERE (c.consultant_id = :consultantId " +
                        "AND c.reservation_date = :reservationDate " +
                        "AND c.reservation_start_time = :reservationStartTime " +
                        "AND c.consulting_status != 'PAYMENT_READY' " +
                        "AND c.is_canceled = false)", nativeQuery = true)
    Long countByReservationDateAndReservationStartTime(@Param("consultantId") Long consultantId,
                                                       @Param("reservationDate") LocalDate reservationDate,
                                                       @Param("reservationStartTime") LocalTime reservationStartTime);

    List<ConsultingReservationInfo> findByUserIdOrderByReservationDateDescReservationStartTimeDesc(Long userId);

    @Query(value = "SELECT * " +
                    "FROM consulting_reservation_info c " +
                    "WHERE (c.user_id = :userId " +
                    "AND c.consulting_status != 'PAYMENT_READY' " +
                    "AND c.is_canceled = false) " +
                    "ORDER BY c.reservation_date DESC, c.reservation_start_time DESC", nativeQuery = true)
    List<ConsultingReservationInfo> findUserReservationInfoList(Long userId);

    @Query(value = "SELECT * " +
                    "FROM consulting_reservation_info c " +
                    "WHERE (c.consultant_id = :consultantId " +
                        "AND c.reservation_date = :reservationDate " +
                        "AND c.is_canceled = false)", nativeQuery = true)
    List<ConsultingReservationInfo> findByReservationDate(@Param("consultantId") Long consultantId,
                                                          @Param("reservationDate") LocalDate reservationDate);
}
