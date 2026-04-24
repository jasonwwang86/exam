import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandidateAnsweringPage } from './CandidateAnsweringPage';
import type { CandidateAnswerSession, CandidateSaveAnswerResult } from '../types';

function buildSession(overrides?: Partial<CandidateAnswerSession>): CandidateAnswerSession {
  return {
    planId: 1,
    name: 'Java 在线答题场次',
    paperName: 'Java 基础试卷',
    durationMinutes: 120,
    sessionStatus: 'IN_PROGRESS',
    startedAt: '2026-05-01T09:10:00',
    deadlineAt: '2026-05-01T10:00:00',
    remainingSeconds: 3000,
    answeredCount: 0,
    totalQuestionCount: 2,
    questions: [
      {
        paperQuestionId: 1,
        questionId: 1,
        questionNo: 1,
        stem: 'Java 的入口方法是什么？',
        questionTypeName: '单选题',
        answerMode: 'SINGLE_CHOICE',
        answerConfig: {
          options: [
            { key: 'A', content: 'main' },
            { key: 'B', content: 'run' },
          ],
        },
        savedAnswer: null,
        answerStatus: 'UNANSWERED',
      },
      {
        paperQuestionId: 2,
        questionId: 2,
        questionNo: 2,
        stem: '请写出 JVM 的英文全称。',
        questionTypeName: '简答题',
        answerMode: 'TEXT',
        answerConfig: {},
        savedAnswer: null,
        answerStatus: 'UNANSWERED',
      },
    ],
    ...overrides,
  };
}

describe('CandidateAnsweringPage', () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders restored answer session content', () => {
    render(
      <CandidateAnsweringPage
        session={buildSession({
          questions: [
            {
              paperQuestionId: 1,
              questionId: 1,
              questionNo: 1,
              stem: 'Java 的入口方法是什么？',
              questionTypeName: '单选题',
              answerMode: 'SINGLE_CHOICE',
              answerConfig: {
                options: [
                  { key: 'A', content: 'main' },
                  { key: 'B', content: 'run' },
                ],
              },
              savedAnswer: {
                selectedOption: 'A',
              },
              answerStatus: 'ANSWERED',
            },
            {
              paperQuestionId: 2,
              questionId: 2,
              questionNo: 2,
              stem: '请写出 JVM 的英文全称。',
              questionTypeName: '简答题',
              answerMode: 'TEXT',
              answerConfig: {},
              savedAnswer: null,
              answerStatus: 'UNANSWERED',
            },
          ],
          answeredCount: 1,
        })}
        submitting={false}
        errorMessage=""
        onSaveAnswer={vi.fn<() => Promise<CandidateSaveAnswerResult>>()}
        onBackToExamList={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Java 在线答题场次' })).toBeInTheDocument();
    expect(screen.getByText('当前已答 1 / 2 题')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'main' })).toBeChecked();
  });

  it('saves current question before switching to another question', async () => {
    const user = userEvent.setup();
    const onSaveAnswer = vi.fn().mockResolvedValue({
      paperQuestionId: 1,
      answerStatus: 'ANSWERED',
      lastSavedAt: '2026-05-01T09:15:00',
      remainingSeconds: 2800,
      sessionStatus: 'IN_PROGRESS',
      answeredCount: 1,
    } satisfies CandidateSaveAnswerResult);

    render(
      <CandidateAnsweringPage
        session={buildSession()}
        submitting={false}
        errorMessage=""
        onSaveAnswer={onSaveAnswer}
        onBackToExamList={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('radio', { name: 'main' }));
    await user.click(screen.getByRole('button', { name: '第 2 题 未答' }));

    await waitFor(() => {
      expect(onSaveAnswer).toHaveBeenCalledWith(1, {
        selectedOption: 'A',
      });
    });
    expect(screen.getByText('当前已答 1 / 2 题')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: '简答题答案' })).toBeInTheDocument();
  });

  it('turns read-only when countdown reaches zero', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-05-01T09:00:00'));

    render(
      <CandidateAnsweringPage
        session={buildSession({
          deadlineAt: '2026-05-01T09:00:02',
          remainingSeconds: 2,
        })}
        submitting={false}
        errorMessage=""
        onSaveAnswer={vi.fn<() => Promise<CandidateSaveAnswerResult>>()}
        onBackToExamList={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await vi.advanceTimersByTimeAsync(3000);

    expect(screen.getAllByText('答题时间已结束')).toHaveLength(2);
    expect(screen.getByRole('button', { name: '保存当前答案' })).toBeDisabled();
    expect(screen.getByRole('radio', { name: 'main' })).toBeDisabled();
  });
});
