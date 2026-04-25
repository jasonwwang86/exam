import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CandidateScoreReportPage } from './CandidateScoreReportPage';
import type { CandidateScoreReport } from '../types';

function buildReport(): CandidateScoreReport {
  return {
    planId: 1,
    name: 'Java 成绩单场次',
    paperName: 'Java 基础试卷',
    durationMinutes: 120,
    remark: '可查看成绩',
    scoreStatus: 'PUBLISHED',
    totalScore: 11,
    objectiveScore: 5,
    subjectiveScore: 6,
    answeredCount: 2,
    unansweredCount: 0,
    submittedAt: '2026-05-01T09:40:00',
    generatedAt: '2026-05-01T09:40:05',
    publishedAt: '2026-05-01T09:40:05',
    submissionMethod: 'MANUAL',
    items: [
      {
        paperQuestionId: 1,
        questionId: 1,
        questionNo: 1,
        questionStem: 'Java 的入口方法是什么？',
        questionTypeName: '单选题',
        answerMode: 'SINGLE_CHOICE',
        answerConfig: {
          options: [
            { key: 'A', content: 'main' },
            { key: 'B', content: 'run' },
          ],
        },
        itemScore: 5,
        awardedScore: 5,
        answerStatus: 'ANSWERED',
        answerSummary: '选择 A',
        savedAnswer: {
          selectedOption: 'A',
        },
        judgeStatus: 'CORRECT',
      },
      {
        paperQuestionId: 2,
        questionId: 2,
        questionNo: 2,
        questionStem: '请写出 JVM 的英文全称。',
        questionTypeName: '简答题',
        answerMode: 'TEXT',
        answerConfig: {},
        itemScore: 6,
        awardedScore: 6,
        answerStatus: 'ANSWERED',
        answerSummary: 'Java Virtual Machine',
        savedAnswer: {
          textAnswer: 'Java Virtual Machine',
        },
        judgeStatus: 'CORRECT',
      },
    ],
  };
}

describe('CandidateScoreReportPage', () => {
  it('renders readonly review content for single choice and text questions', () => {
    render(
      <CandidateScoreReportPage
        report={buildReport()}
        errorMessage=""
        onBackToExamList={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByRole('radio', { name: 'main' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'main' })).toBeDisabled();
    expect(screen.getByRole('radio', { name: 'run' })).not.toBeChecked();
    expect(screen.getByRole('textbox', { name: '第 2 题作答内容' })).toHaveValue('Java Virtual Machine');
    expect(screen.getByRole('textbox', { name: '第 2 题作答内容' })).toBeDisabled();
    expect(screen.getAllByText('判定正确')).toHaveLength(2);
  });
});
