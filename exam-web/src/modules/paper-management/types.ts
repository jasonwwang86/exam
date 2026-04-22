import type { QuestionDifficulty, QuestionListResponse } from '../question-bank/types';

export type PaperListRecord = {
  id: number;
  name: string;
  totalScore: number;
  durationMinutes: number;
  questionCount: number;
  updatedAt: string;
};

export type PaperDetailRecord = {
  id: number;
  name: string;
  description: string;
  durationMinutes: number;
  totalScore: number;
  questionCount: number;
  remark: string;
  updatedAt: string;
};

export type PaperQuestionRecord = {
  id: number;
  questionId: number;
  questionStemSnapshot: string;
  questionTypeNameSnapshot: string;
  difficultySnapshot: QuestionDifficulty;
  itemScore: number;
  displayOrder: number;
  updatedAt: string;
};

export type PaperListParams = {
  keyword?: string;
  page?: number;
  pageSize?: number;
};

export type PaperListResponse = {
  total: number;
  page: number;
  pageSize: number;
  records: PaperListRecord[];
};

export type PaperPayload = {
  name: string;
  description: string;
  durationMinutes: number;
  remark: string;
};

export type AddPaperQuestionsPayload = {
  questionIds: number[];
};

export type UpdatePaperQuestionPayload = {
  itemScore: number;
  displayOrder: number;
};

export type CandidateQuestionResponse = QuestionListResponse;
