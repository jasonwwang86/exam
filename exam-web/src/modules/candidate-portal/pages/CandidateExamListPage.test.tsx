import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CandidateExamListPage } from './CandidateExamListPage';

describe('CandidateExamListPage', () => {
  it('shows submitted summary and hides answer entry for submitted exam', () => {
    render(
      <CandidateExamListPage
        exams={
          [
            {
              planId: 1,
              name: 'Java 在线答题场次',
              paperName: 'Java 基础试卷',
              durationMinutes: 120,
              startTime: '2026-05-01T09:00:00',
              endTime: '2026-05-01T12:00:00',
              displayStatus: '进行中',
              remark: '已交卷',
              canEnterAnswering: false,
              answeringStatus: 'SUBMITTED',
              remainingSeconds: 0,
              submittedAt: '2026-05-01T09:40:00',
              submissionMethod: 'MANUAL',
            },
          ] as any
        }
        submitting={false}
        errorMessage=""
        onRefresh={vi.fn()}
        onEnterAnswering={vi.fn()}
        onViewScoreReport={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByText('已提交')).toBeInTheDocument();
    expect(screen.getByText('提交时间 2026-05-01T09:40:00')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '进入答题' })).not.toBeInTheDocument();
  });

  it('shows score summary and report entry when score report is available', () => {
    render(
      <CandidateExamListPage
        exams={
          [
            {
              planId: 1,
              name: 'Java 成绩单场次',
              paperName: 'Java 基础试卷',
              durationMinutes: 120,
              startTime: '2026-05-01T09:00:00',
              endTime: '2026-05-01T12:00:00',
              displayStatus: '已结束',
              remark: '可查看成绩',
              canEnterAnswering: false,
              answeringStatus: 'SUBMITTED',
              remainingSeconds: 0,
              submittedAt: '2026-05-01T09:40:00',
              submissionMethod: 'MANUAL',
              scoreStatus: 'PUBLISHED',
              reportAvailable: true,
              totalScore: 92.5,
              resultGeneratedAt: '2026-05-01T10:00:00',
            },
          ] as any
        }
        submitting={false}
        errorMessage=""
        onRefresh={vi.fn()}
        onEnterAnswering={vi.fn()}
        onViewScoreReport={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByText('总分 92.5')).toBeInTheDocument();
    expect(screen.getByText('已出分')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '查看成绩' })).toBeInTheDocument();
  });
});
