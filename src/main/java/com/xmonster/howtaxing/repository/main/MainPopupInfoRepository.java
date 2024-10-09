package com.xmonster.howtaxing.repository.main;

import com.xmonster.howtaxing.model.MainPopupInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MainPopupInfoRepository extends JpaRepository<MainPopupInfo, Long> {
    @Query(value = "SELECT * FROM main_popup_info m WHERE m.is_post = true", nativeQuery = true)
    List<MainPopupInfo> findByIsPost();
}
