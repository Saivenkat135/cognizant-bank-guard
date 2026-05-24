package com.cts.gemini_test_try2.Controller;

import com.cts.gemini_test_try2.Service.GeminiService;
import com.cts.gemini_test_try2.dto.DecisionRequest;
import com.cts.gemini_test_try2.dto.GeminiDecisionResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
class GeminiController{

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/analyze-transaction")
    public ResponseEntity<GeminiDecisionResponse> analyzeTransaction(@RequestBody DecisionRequest decisionRequest) throws HttpException, IOException {
        GeminiDecisionResponse response = geminiService.analyzeTransactionWithGemini(decisionRequest);
        return ResponseEntity.ok(response);
    }

}