import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockListQuestions,
  mockGetQuestion,
  mockCreateQuestion,
  mockUpdateQuestion,
  mockDeleteQuestion,
  mockListQuestionTypes,
  mockCreateQuestionType,
  mockUpdateQuestionType,
  mockDeleteQuestionType,
} = vi.hoisted(() => ({
  mockListQuestions: vi.fn(),
  mockGetQuestion: vi.fn(),
  mockCreateQuestion: vi.fn(),
  mockUpdateQuestion: vi.fn(),
  mockDeleteQuestion: vi.fn(),
  mockListQuestionTypes: vi.fn(),
  mockCreateQuestionType: vi.fn(),
  mockUpdateQuestionType: vi.fn(),
  mockDeleteQuestionType: vi.fn(),
}));

vi.mock('../services/questionApi', () => ({
  listQuestions: mockListQuestions,
  getQuestion: mockGetQuestion,
  createQuestion: mockCreateQuestion,
  updateQuestion: mockUpdateQuestion,
  deleteQuestion: mockDeleteQuestion,
  listQuestionTypes: mockListQuestionTypes,
  createQuestionType: mockCreateQuestionType,
  updateQuestionType: mockUpdateQuestionType,
  deleteQuestionType: mockDeleteQuestionType,
}));

describe('QuestionBankManagementPage', () => {
  beforeEach(() => {
    mockListQuestions.mockReset();
    mockGetQuestion.mockReset();
    mockCreateQuestion.mockReset();
    mockUpdateQuestion.mockReset();
    mockDeleteQuestion.mockReset();
    mockListQuestionTypes.mockReset();
    mockCreateQuestionType.mockReset();
    mockUpdateQuestionType.mockReset();
    mockDeleteQuestionType.mockReset();

    mockListQuestionTypes.mockResolvedValue([
      { id: 1, name: '单选题', answerMode: 'SINGLE_CHOICE', sort: 10, remark: '唯一正确答案' },
      { id: 3, name: '判断题', answerMode: 'TRUE_FALSE', sort: 30, remark: '布尔型答案' },
      { id: 4, name: '简答题', answerMode: 'TEXT', sort: 40, remark: '文本参考答案' },
    ]);
    mockListQuestions.mockResolvedValue({
      total: 1,
      page: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          stem: 'Java 的入口方法是什么？',
          questionTypeId: 1,
          questionTypeName: '单选题',
          difficulty: 'EASY',
          score: 5,
          updatedAt: '2026-04-22T10:00:00',
        },
      ],
    });
    mockGetQuestion.mockResolvedValue({
      id: 1,
      stem: 'Java 的入口方法是什么？',
      questionTypeId: 1,
      questionTypeName: '单选题',
      answerMode: 'SINGLE_CHOICE',
      difficulty: 'EASY',
      score: 5,
      answerConfig: {
        options: [
          { key: 'A', content: 'main' },
          { key: 'B', content: 'run' },
        ],
        correctOption: 'A',
      },
      updatedAt: '2026-04-22T10:00:00',
    });
    mockCreateQuestion.mockResolvedValue({});
    mockUpdateQuestion.mockResolvedValue({});
    mockDeleteQuestion.mockResolvedValue({});
    mockCreateQuestionType.mockResolvedValue({});
    mockUpdateQuestionType.mockResolvedValue({});
    mockDeleteQuestionType.mockResolvedValue({});
    vi.stubGlobal('confirm', vi.fn(() => true));
  });

  it('loads questions and supports querying by filters', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={['question:read', 'question-type:read']}
      />,
    );

    expect(await screen.findByRole('heading', { name: '题库管理' })).toBeInTheDocument();
    expect(screen.getByText('题目列表')).toBeInTheDocument();
    expect(screen.getByText('Java 的入口方法是什么？')).toBeInTheDocument();

    await user.type(screen.getByLabelText('关键字'), '入口');
    await user.selectOptions(screen.getByLabelText('题型'), '1');
    await user.selectOptions(screen.getByLabelText('难度'), 'EASY');
    await user.click(screen.getByRole('button', { name: /查\s*询/ }));

    await waitFor(() => {
      expect(mockListQuestions).toHaveBeenLastCalledWith('token-123', {
        keyword: '入口',
        questionTypeId: 1,
        difficulty: 'EASY',
        page: 1,
        pageSize: 10,
      });
    });
  });

  it('validates required question fields before creating', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={['question:read', 'question:create', 'question-type:read']}
      />,
    );

    await screen.findByRole('heading', { name: '题库管理' });
    await user.click(screen.getByRole('button', { name: /新\s*增/ }));
    await user.click(await screen.findByText('新增题目'));
    await user.click(screen.getByRole('button', { name: /创\s*建/ }));

    expect(screen.getByRole('alert')).toHaveTextContent('请填写题干、题型、难度、分值和答案配置');
    expect(mockCreateQuestion).not.toHaveBeenCalled();
  });

  it('uses four default choice options and supports adding or removing options when creating', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={['question:read', 'question:create', 'question-type:read']}
      />,
    );

    await screen.findByRole('heading', { name: '题库管理' });
    await user.click(screen.getByRole('button', { name: /新\s*增/ }));
    await user.click(await screen.findByText('新增题目'));

    expect(await screen.findByRole('dialog', { name: '新增题目' })).toBeInTheDocument();
    expect(screen.getByLabelText('选项 A')).toBeInTheDocument();
    expect(screen.getByLabelText('选项 B')).toBeInTheDocument();
    expect(screen.getByLabelText('选项 C')).toBeInTheDocument();
    expect(screen.getByLabelText('选项 D')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '新增选项' }));
    expect(screen.getByLabelText('选项 E')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '删除选项 E' }));
    await waitFor(() => {
      expect(screen.queryByLabelText('选项 E')).not.toBeInTheDocument();
    });
  });

  it('opens edit modal, loads detail and updates question', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={['question:read', 'question:update', 'question-type:read']}
      />,
    );

    await screen.findByText('Java 的入口方法是什么？');
    await user.click(screen.getByRole('button', { name: /编\s*辑/ }));

    expect(mockGetQuestion).toHaveBeenCalledWith('token-123', 1);
    expect(await screen.findByRole('dialog', { name: '编辑题目' })).toBeInTheDocument();

    await user.clear(screen.getByLabelText('题干'));
    await user.type(screen.getByLabelText('题干'), 'JVM 的英文全称是什么？');
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(mockUpdateQuestion).toHaveBeenCalledWith('token-123', 1, {
        stem: 'JVM 的英文全称是什么？',
        questionTypeId: 1,
        difficulty: 'EASY',
        score: 5,
        answerConfig: {
          options: [
            { key: 'A', content: 'main' },
            { key: 'B', content: 'run' },
          ],
          correctOption: 'A',
        },
      });
    });
  });

  it('deletes question after confirmation', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={['question:read', 'question:delete', 'question-type:read']}
      />,
    );

    await screen.findByText('Java 的入口方法是什么？');
    await user.click(screen.getByRole('button', { name: /删\s*除/ }));

    await waitFor(() => {
      expect(mockDeleteQuestion).toHaveBeenCalledWith('token-123', 1);
    });
  });

  it('supports managing question types', async () => {
    const { QuestionBankManagementPage } = await import('./QuestionBankManagementPage');
    const user = userEvent.setup();

    render(
      <QuestionBankManagementPage
        token="token-123"
        permissions={[
          'question:read',
          'question:create',
          'question-type:read',
          'question-type:create',
          'question-type:update',
          'question-type:delete',
        ]}
      />,
    );

    await screen.findByRole('heading', { name: '题库管理' });
    await user.click(screen.getByRole('button', { name: /新\s*增/ }));
    await user.click(await screen.findByText('题型管理'));

    expect(await screen.findByRole('dialog', { name: '题型管理' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('题型名称'), '填空题');
    await user.selectOptions(screen.getByLabelText('答案模式'), 'TEXT');
    await user.clear(screen.getByLabelText('排序'));
    await user.type(screen.getByLabelText('排序'), '50');
    await user.type(screen.getByLabelText('题型备注'), '文本填空');
    await user.click(screen.getByRole('button', { name: '保存题型' }));

    await waitFor(() => {
      expect(mockCreateQuestionType).toHaveBeenCalledWith('token-123', {
        name: '填空题',
        answerMode: 'TEXT',
        sort: 50,
        remark: '文本填空',
      });
    });
  });
});
