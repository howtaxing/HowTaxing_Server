package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.ReviewType;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Review extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long reviewId;              // 리뷰ID

    private Long userId;                // 사용자ID

    @Enumerated(EnumType.STRING)
    private ReviewType reviewType;      // 리뷰유형

    private Integer score;              // 평점
    private String reviewContents;      // 리뷰내용
}