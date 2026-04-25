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
  submittedAt?: string;
  submissionMethod?: string;
  scoreStatus?: string | null;
  reportAvailable?: boolean;
  totalScore?: number | null;
  resultGeneratedAt?: string | null;
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
  submittedAt?: string;
  submissionMethod?: string;
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

export type CandidateExamSubmissionResult = {
  planId: number;
  name: string;
  paperName: string;
  sessionStatus: string;
  submissionMethod: string;
  submittedAt: string;
  answeredCount: number;
  totalQuestionCount: number;
};

export type CandidateScoreReportItem = {
  paperQuestionId: number;
  questionId: number;
  questionNo: number;
  questionStem: string;
  questionTypeName: string;
  answerMode: string;
  answerConfig: {
    options?: CandidateQuestionOption[];
    [key: string]: unknown;
  };
  itemScore: number;
  awardedScore: number;
  answerStatus: string;
  answerSummary: string | null;
  savedAnswer: Record<string, unknown> | null;
  judgeStatus: string;
};

export type CandidateScoreReport = {
  planId: number;
  name: string;
  paperName: string;
  durationMinutes: number;
  remark: string | null;
  scoreStatus: string;
  totalScore: number;
  objectiveScore?: number | null;
  subjectiveScore?: number | null;
  answeredCount: number;
  unansweredCount: number;
  submittedAt: string;
  generatedAt: string;
  publishedAt?: string | null;
  submissionMethod: string;
  items: CandidateScoreReportItem[];
};
