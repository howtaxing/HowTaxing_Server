package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MainPopupInfo extends DateEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long mainPopupId;                   // 메인팝업ID

    private String mainPopupContent;            // 메인팝업내용
    private String targetUrl;                   // 타겟URL
    private String imageUrl;                    // 이미지URL
    private Boolean isPost;                     // 게시여부
    private LocalDateTime postStartDatetime;    // 게시시작일시
    private LocalDateTime postEndDatetime;      // 게시종료일시
    private String remark;                      // 비고
}
