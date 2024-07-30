package com.xmonster.howtaxing.dto.question;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class AdditionalQuestionResponse {
    private Boolean hasNextQuestion;        // 다음질의존재여부
    private String nextQuestionId;          // 다음질의ID
    private String nextQuestionContent;     // 다음질의내용
    private Boolean isNeedAnswer;           // 응답필요여부
    private String answerType;              // 응답유형

    // 응답선택목록
    private List<AnswerSelectListResponse> answerSelectList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AnswerSelectListResponse {
        private String answerValue;         // 응답값
        private String answerContent;       // 응답내용
    }
}
