export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type QuestionAnswerMode = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'TEXT';

export type QuestionTypeRecord = {
  id: number;
  name: string;
  answerMode: QuestionAnswerMode;
  sort: number;
  remark: string;
};

export type QuestionListRecord = {
  id: number;
  stem: string;
  questionTypeId: number;
  questionTypeName: string;
  difficulty: QuestionDifficulty;
  score: number;
  updatedAt: string;
};

export type ChoiceOption = {
  key: string;
  content: string;
};

export type QuestionAnswerConfig =
  | {
      options: ChoiceOption[];
      correctOption: string;
    }
  | {
      options: ChoiceOption[];
      correctOptions: string[];
    }
  | {
      correctAnswer: boolean;
    }
  | {
      acceptedAnswers: string[];
    };

export type QuestionDetailRecord = {
  id: number;
  stem: string;
  questionTypeId: number;
  questionTypeName: string;
  answerMode: QuestionAnswerMode;
  difficulty: QuestionDifficulty;
  score: number;
  answerConfig: QuestionAnswerConfig;
  updatedAt: string;
};

export type QuestionListParams = {
  keyword?: string;
  questionTypeId?: number;
  difficulty?: QuestionDifficulty | '';
  page?: number;
  pageSize?: number;
};

export type QuestionListResponse = {
  total: number;
  page: number;
  pageSize: number;
  records: QuestionListRecord[];
};

export type QuestionPayload = {
  stem: string;
  questionTypeId: number;
  difficulty: QuestionDifficulty;
  score: number;
  answerConfig: QuestionAnswerConfig;
};

export type QuestionTypePayload = {
  name: string;
  answerMode: QuestionAnswerMode;
  sort: number;
  remark: string;
};
