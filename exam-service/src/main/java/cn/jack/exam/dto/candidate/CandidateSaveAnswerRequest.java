package cn.jack.exam.dto.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CandidateSaveAnswerRequest {

    private JsonNode answerContent;
}
