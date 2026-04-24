export type CandidateLoginFormState = {
  examineeNo: string;
  idCardNo: string;
};

export type CandidateProfileSummary = {
  examineeId: number;
  examineeNo: string;
  name: string;
  maskedIdCardNo: string;
};

export type CandidateProfile = CandidateProfileSummary & {
  profileConfirmed: boolean;
  message: string;
};

export type CandidateExam = {
  planId: number;
  name: string;
  paperName: string;
  durationMinutes: number;
  startTime: string;
  endTime: string;
  displayStatus: string;
  remark: string | null;
  canEnterAnswering: boolean;
  answeringStatus: string;
  remainingSeconds: number | null;
};

export type CandidateQuestionOption = {
  key: string;
  content: string;
};

export type CandidateAnswerQuestion = {
  paperQuestionId: number;
  questionId: number;
  questionNo: number;
  stem: string;
  questionTypeName: string;
  answerMode: string;
  answerConfig: {
    options?: CandidateQuestionOption[];
    [key: string]: unknown;
  };
  savedAnswer: Record<string, unknown> | null;
  answerStatus: string;
};

export type CandidateAnswerSession = {
  planId: number;
  name: string;
  paperName: string;
  durationMinutes: number;
  sessionStatus: string;
  startedAt: string;
  deadlineAt: string;
  remainingSeconds: number;
  answeredCount: number;
  totalQuestionCount: number;
  questions: CandidateAnswerQuestion[];
};

export type CandidateSaveAnswerResult = {
  paperQuestionId: number;
  answerStatus: string;
  lastSavedAt: string;
  remainingSeconds: number;
  sessionStatus: string;
  answeredCount: number;
};
