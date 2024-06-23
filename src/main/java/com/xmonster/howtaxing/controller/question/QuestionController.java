package com.xmonster.howtaxing.controller.question;

import com.xmonster.howtaxing.dto.question.AdditionalQuestionRequest;
import com.xmonster.howtaxing.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QuestionController {
    private final QuestionService questionService;

    // 추가질의항목 조회
    @PostMapping("/question/additionalQuestion")
    public Object getAdditionalQuestion(@RequestBody AdditionalQuestionRequest additionalQuestionRequest) throws Exception {
        log.info(">> [Controller]QuestionController getAdditionalQuestion - 추가질의항목 조회");
        return questionService.getAdditionalQuestion(additionalQuestionRequest);
    }
}
