import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockListPapers,
  mockGetPaper,
  mockCreatePaper,
  mockUpdatePaper,
  mockDeletePaper,
  mockListPaperQuestions,
  mockAddPaperQuestions,
  mockUpdatePaperQuestion,
  mockDeletePaperQuestion,
  mockListQuestions,
  mockListQuestionTypes,
} = vi.hoisted(() => ({
  mockListPapers: vi.fn(),
  mockGetPaper: vi.fn(),
  mockCreatePaper: vi.fn(),
  mockUpdatePaper: vi.fn(),
  mockDeletePaper: vi.fn(),
  mockListPaperQuestions: vi.fn(),
  mockAddPaperQuestions: vi.fn(),
  mockUpdatePaperQuestion: vi.fn(),
  mockDeletePaperQuestion: vi.fn(),
  mockListQuestions: vi.fn(),
  mockListQuestionTypes: vi.fn(),
}));

vi.mock('../services/paperApi', () => ({
  listPapers: mockListPapers,
  getPaper: mockGetPaper,
  createPaper: mockCreatePaper,
  updatePaper: mockUpdatePaper,
  deletePaper: mockDeletePaper,
  listPaperQuestions: mockListPaperQuestions,
  addPaperQuestions: mockAddPaperQuestions,
  updatePaperQuestion: mockUpdatePaperQuestion,
  deletePaperQuestion: mockDeletePaperQuestion,
}));

vi.mock('../../question-bank/services/questionApi', () => ({
  listQuestions: mockListQuestions,
  listQuestionTypes: mockListQuestionTypes,
}));

describe('PaperManagementPage', () => {
  beforeEach(() => {
    mockListPapers.mockReset();
    mockGetPaper.mockReset();
    mockCreatePaper.mockReset();
    mockUpdatePaper.mockReset();
    mockDeletePaper.mockReset();
    mockListPaperQuestions.mockReset();
    mockAddPaperQuestions.mockReset();
    mockUpdatePaperQuestion.mockReset();
    mockDeletePaperQuestion.mockReset();
    mockListQuestions.mockReset();
    mockListQuestionTypes.mockReset();

    mockListPapers.mockResolvedValue({
      total: 1,
      page: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          name: 'Java 基础试卷',
          totalScore: 11,
          durationMinutes: 120,
          questionCount: 2,
          updatedAt: '2026-04-22T10:00:00',
        },
      ],
    });
    mockGetPaper.mockResolvedValue({
      id: 1,
      name: 'Java 基础试卷',
      description: '覆盖 Java 基础知识',
      durationMinutes: 120,
      totalScore: 11,
      questionCount: 2,
      remark: '首套试卷',
      updatedAt: '2026-04-22T10:00:00',
    });
    mockListPaperQuestions.mockResolvedValue([
      {
        id: 8,
        questionId: 1,
        questionStemSnapshot: 'Java 的入口方法是什么？',
        questionTypeNameSnapshot: '单选题',
        difficultySnapshot: 'EASY',
        itemScore: 5,
        displayOrder: 1,
        updatedAt: '2026-04-22T10:00:00',
      },
      {
        id: 9,
        questionId: 2,
        questionStemSnapshot: '请写出 JVM 的英文全称。',
        questionTypeNameSnapshot: '简答题',
        difficultySnapshot: 'MEDIUM',
        itemScore: 6,
        displayOrder: 2,
        updatedAt: '2026-04-22T10:10:00',
      },
    ]);
    mockListQuestionTypes.mockResolvedValue([
      { id: 1, name: '单选题', answerMode: 'SINGLE_CHOICE', sort: 10, remark: '唯一正确答案' },
      { id: 4, name: '简答题', answerMode: 'TEXT', sort: 40, remark: '文本参考答案' },
    ]);
    mockListQuestions.mockResolvedValue({
      total: 1,
      page: 1,
      pageSize: 10,
      records: [
        {
          id: 3,
          stem: 'Java 是解释型语言。',
          questionTypeId: 1,
          questionTypeName: '判断题',
          difficulty: 'EASY',
          score: 2,
          updatedAt: '2026-04-22T11:00:00',
        },
      ],
    });
    mockCreatePaper.mockResolvedValue({});
    mockUpdatePaper.mockResolvedValue({});
    mockDeletePaper.mockResolvedValue({});
    mockAddPaperQuestions.mockResolvedValue([]);
    mockUpdatePaperQuestion.mockResolvedValue({});
    mockDeletePaperQuestion.mockResolvedValue({});
    vi.stubGlobal('confirm', vi.fn(() => true));
  });

  it('loads papers and supports querying by keyword', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={['paper:read', 'paper-question:read']}
      />,
    );

    expect(await screen.findByRole('heading', { name: '试卷管理' })).toBeInTheDocument();
    expect(screen.getByText('Java 基础试卷')).toBeInTheDocument();

    await user.type(screen.getByLabelText('关键字'), 'Java');
    await user.click(screen.getByRole('button', { name: /查\s*询/ }));

    await waitFor(() => {
      expect(mockListPapers).toHaveBeenLastCalledWith('token-123', {
        keyword: 'Java',
        page: 1,
        pageSize: 10,
      });
    });
  });

  it('validates required paper fields before creating', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={['paper:read', 'paper:create', 'paper-question:read']}
      />,
    );

    await screen.findByRole('heading', { name: '试卷管理' });
    await user.click(screen.getByRole('button', { name: '新增试卷' }));
    await user.click(screen.getByRole('button', { name: '创建试卷' }));

    expect(screen.getByRole('alert')).toHaveTextContent('请填写试卷名称和考试时长');
    expect(mockCreatePaper).not.toHaveBeenCalled();
  });

  it('opens edit dialog, loads detail and updates paper', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={['paper:read', 'paper:update', 'paper-question:read']}
      />,
    );

    await screen.findByText('Java 基础试卷');
    await user.click(screen.getByRole('button', { name: '编辑基础信息' }));

    expect(mockGetPaper).toHaveBeenCalledWith('token-123', 1);
    expect(await screen.findByRole('dialog', { name: '编辑试卷' })).toBeInTheDocument();

    await user.clear(screen.getByLabelText('试卷名称'));
    await user.type(screen.getByLabelText('试卷名称'), 'Java 基础试卷-更新');
    await user.clear(screen.getByLabelText('考试时长（分钟）'));
    await user.type(screen.getByLabelText('考试时长（分钟）'), '95');
    await user.click(screen.getByRole('button', { name: '保存试卷' }));

    await waitFor(() => {
      expect(mockUpdatePaper).toHaveBeenCalledWith('token-123', 1, {
        name: 'Java 基础试卷-更新',
        description: '覆盖 Java 基础知识',
        durationMinutes: 95,
        remark: '首套试卷',
      });
    });
  });

  it('deletes paper after confirmation', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={['paper:read', 'paper:delete', 'paper-question:read']}
      />,
    );

    await screen.findByText('Java 基础试卷');
    await user.click(screen.getByRole('button', { name: '删除试卷' }));

    await waitFor(() => {
      expect(mockDeletePaper).toHaveBeenCalledWith('token-123', 1);
    });
  });

  it('supports hand-picking questions into a paper', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={[
          'paper:read',
          'paper-question:read',
          'paper-question:create',
        ]}
      />,
    );

    await screen.findByText('Java 基础试卷');
    await user.click(screen.getByRole('button', { name: '维护题目' }));

    expect(await screen.findByRole('dialog', { name: '维护试卷题目' })).toBeInTheDocument();
    expect(mockListPaperQuestions).toHaveBeenCalledWith('token-123', 1);
    expect(screen.getByText('当前总分：11')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '手工组卷' }));
    expect(await screen.findByRole('dialog', { name: '手工组选题' })).toBeInTheDocument();
    expect(mockListQuestionTypes).toHaveBeenCalledWith('token-123');
    expect(mockListQuestions).toHaveBeenCalledWith('token-123', {
      keyword: '',
      questionTypeId: undefined,
      difficulty: undefined,
      page: 1,
      pageSize: 10,
    });

    await user.click(screen.getByRole('checkbox', { name: '选择题目 3' }));
    await user.click(screen.getByRole('button', { name: '加入试卷' }));

    await waitFor(() => {
      expect(mockAddPaperQuestions).toHaveBeenCalledWith('token-123', 1, {
        questionIds: [3],
      });
    });
  });

  it('supports updating and removing paper questions', async () => {
    const { PaperManagementPage } = await import('./PaperManagementPage');
    const user = userEvent.setup();

    render(
      <PaperManagementPage
        token="token-123"
        permissions={[
          'paper:read',
          'paper-question:read',
          'paper-question:update',
          'paper-question:delete',
        ]}
      />,
    );

    await screen.findByText('Java 基础试卷');
    await user.click(screen.getByRole('button', { name: '维护题目' }));

    expect(await screen.findByRole('dialog', { name: '维护试卷题目' })).toBeInTheDocument();
    await user.clear(screen.getByLabelText('题目 9 分值'));
    await user.type(screen.getByLabelText('题目 9 分值'), '10');
    await user.click(screen.getByRole('button', { name: '保存题目 9' }));

    await waitFor(() => {
      expect(mockUpdatePaperQuestion).toHaveBeenCalledWith('token-123', 1, 9, {
        itemScore: 10,
        displayOrder: 2,
      });
    });

    await user.click(screen.getByRole('button', { name: '移除题目 9' }));
    await waitFor(() => {
      expect(mockDeletePaperQuestion).toHaveBeenCalledWith('token-123', 1, 9);
    });
  });
});
