import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Dropdown, Empty, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography } from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createQuestion,
  createQuestionType,
  deleteQuestion,
  deleteQuestionType,
  getQuestion,
  listQuestionTypes,
  listQuestions,
  updateQuestion,
  updateQuestionType,
} from '../services/questionApi';
import type {
  ChoiceOption,
  QuestionAnswerMode,
  QuestionDetailRecord,
  QuestionDifficulty,
  QuestionListRecord,
  QuestionPayload,
  QuestionTypePayload,
  QuestionTypeRecord,
} from '../types';
import { AdminPage, AdminPageHeader, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import styles from './QuestionBankManagementPage.module.css';

type QuestionBankManagementPageProps = {
  token: string;
  permissions: string[];
};

type QuestionFormState = {
  stem: string;
  questionTypeId: string;
  difficulty: QuestionDifficulty;
  score: number;
  answerMode: QuestionAnswerMode;
  options: ChoiceOption[];
  correctOption: string;
  correctOptions: string[];
  correctAnswer: 'true' | 'false';
  acceptedAnswersText: string;
};

type QuestionTypeFormState = {
  name: string;
  answerMode: QuestionAnswerMode;
  sort: number;
  remark: string;
};

const DEFAULT_CHOICE_OPTION_COUNT = 4;
const MIN_CHOICE_OPTION_COUNT = 2;

function createEmptyQuestionForm(): QuestionFormState {
  return {
    stem: '',
    questionTypeId: '',
    difficulty: 'EASY',
    score: 0,
    answerMode: 'SINGLE_CHOICE',
    options: createChoiceOptions(),
    correctOption: '',
    correctOptions: [],
    correctAnswer: 'true',
    acceptedAnswersText: '',
  };
}

const EMPTY_TYPE_FORM: QuestionTypeFormState = {
  name: '',
  answerMode: 'TEXT',
  sort: 10,
  remark: '',
};

export function QuestionBankManagementPage({ token, permissions }: QuestionBankManagementPageProps) {
  const [keyword, setKeyword] = useState('');
  const [questionTypeId, setQuestionTypeId] = useState('');
  const [difficulty, setDifficulty] = useState('');
  const [records, setRecords] = useState<QuestionListRecord[]>([]);
  const [questionTypes, setQuestionTypes] = useState<QuestionTypeRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [formErrorMessage, setFormErrorMessage] = useState('');
  const [typeErrorMessage, setTypeErrorMessage] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [typeManagerOpen, setTypeManagerOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [typeSaving, setTypeSaving] = useState(false);
  const [questionForm, setQuestionForm] = useState<QuestionFormState>(() => createEmptyQuestionForm());
  const [typeForm, setTypeForm] = useState<QuestionTypeFormState>(EMPTY_TYPE_FORM);
  const [editingRecord, setEditingRecord] = useState<QuestionListRecord | null>(null);
  const [editingType, setEditingType] = useState<QuestionTypeRecord | null>(null);

  const canRead = permissions.includes('question:read');
  const canCreate = permissions.includes('question:create');
  const canUpdate = permissions.includes('question:update');
  const canDelete = permissions.includes('question:delete');
  const canReadType = permissions.includes('question-type:read');
  const canCreateType = permissions.includes('question-type:create');
  const canUpdateType = permissions.includes('question-type:update');
  const canDeleteType = permissions.includes('question-type:delete');

  useEffect(() => {
    if (!canRead && !canReadType) {
      setLoading(false);
      return;
    }
    void initializePage();
  }, [canRead, canReadType, token]);

  async function initializePage() {
    setLoading(true);
    setErrorMessage('');
    try {
      if (canReadType) {
        const types = await listQuestionTypes(token);
        setQuestionTypes(types);
      }
      if (canRead) {
        const response = await listQuestions(token, {
          keyword: '',
          page: 1,
          pageSize: 10,
        });
        setRecords(response.records);
      }
    } catch {
      setErrorMessage('题库数据加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  async function loadQuestions(nextFilters: { keyword: string; questionTypeId: string; difficulty: string }) {
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await listQuestions(token, {
        keyword: nextFilters.keyword,
        questionTypeId: nextFilters.questionTypeId ? Number(nextFilters.questionTypeId) : undefined,
        difficulty: (nextFilters.difficulty || undefined) as QuestionDifficulty | undefined,
        page: 1,
        pageSize: 10,
      });
      setRecords(response.records);
    } catch {
      setErrorMessage('题目数据加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  async function refreshQuestionTypes() {
    if (!canReadType) {
      return;
    }
    const types = await listQuestionTypes(token);
    setQuestionTypes(types);
  }

  function resetQuestionForm() {
    setQuestionForm(createEmptyQuestionForm());
    setEditingRecord(null);
    setFormErrorMessage('');
    setSaving(false);
  }

  function resetTypeForm() {
    setTypeForm(EMPTY_TYPE_FORM);
    setEditingType(null);
    setTypeErrorMessage('');
    setTypeSaving(false);
  }

  function openCreateQuestionForm() {
    resetQuestionForm();
    setFormOpen(true);
  }

  async function openEditQuestionForm(record: QuestionListRecord) {
    try {
      const detail = await getQuestion(token, record.id);
      setEditingRecord(record);
      setQuestionForm(fromQuestionDetail(detail));
      setFormErrorMessage('');
      setFormOpen(true);
    } catch {
      setErrorMessage('题目详情加载失败，请稍后重试');
    }
  }

  function closeQuestionModal() {
    setFormOpen(false);
    resetQuestionForm();
  }

  function openTypeManager() {
    resetTypeForm();
    setTypeManagerOpen(true);
  }

  function closeTypeManager() {
    setTypeManagerOpen(false);
    resetTypeForm();
  }

  async function handleQuery() {
    await loadQuestions({ keyword, questionTypeId, difficulty });
  }

  async function handleSaveQuestion() {
    const payload = buildQuestionPayload(questionForm);
    if (!payload) {
      setFormErrorMessage('请填写题干、题型、难度、分值和答案配置');
      return;
    }

    setSaving(true);
    setFormErrorMessage('');
    try {
      if (editingRecord) {
        await updateQuestion(token, editingRecord.id, payload);
      } else {
        await createQuestion(token, payload);
      }
      closeQuestionModal();
      await loadQuestions({ keyword, questionTypeId, difficulty });
    } catch {
      setFormErrorMessage('保存题目失败，请稍后重试');
      setSaving(false);
    }
  }

  async function handleDeleteQuestion(record: QuestionListRecord) {
    if (!window.confirm(`确认删除题目“${record.stem}”吗？`)) {
      return;
    }
    try {
      await deleteQuestion(token, record.id);
      await loadQuestions({ keyword, questionTypeId, difficulty });
    } catch {
      setErrorMessage('删除题目失败，请稍后重试');
    }
  }

  async function handleSaveQuestionType() {
    const payload = buildQuestionTypePayload(typeForm);
    if (!payload) {
      setTypeErrorMessage('请填写题型名称、答案模式和排序');
      return;
    }

    setTypeSaving(true);
    setTypeErrorMessage('');
    try {
      if (editingType) {
        await updateQuestionType(token, editingType.id, payload);
      } else {
        await createQuestionType(token, payload);
      }
      resetTypeForm();
      await refreshQuestionTypes();
    } catch {
      setTypeErrorMessage('保存题型失败，请稍后重试');
      setTypeSaving(false);
    }
  }

  function startEditQuestionType(record: QuestionTypeRecord) {
    setEditingType(record);
    setTypeForm({
      name: record.name,
      answerMode: record.answerMode,
      sort: record.sort,
      remark: record.remark,
    });
    setTypeErrorMessage('');
  }

  async function handleDeleteQuestionType(record: QuestionTypeRecord) {
    if (!window.confirm(`确认删除题型 ${record.name} 吗？`)) {
      return;
    }
    try {
      await deleteQuestionType(token, record.id);
      await refreshQuestionTypes();
    } catch {
      setTypeErrorMessage('删除题型失败，请稍后重试');
    }
  }

  function handleQuestionTypeChange(nextQuestionTypeId: string) {
    const nextType = questionTypes.find((item) => item.id === Number(nextQuestionTypeId));
    setQuestionForm((current) => ({
      ...current,
      questionTypeId: nextQuestionTypeId,
      answerMode: nextType?.answerMode ?? current.answerMode,
      options: nextType?.answerMode === 'SINGLE_CHOICE' || nextType?.answerMode === 'MULTIPLE_CHOICE'
        ? ensureChoiceOptions(current.options)
        : createChoiceOptions(),
      correctOption: '',
      correctOptions: [],
      correctAnswer: 'true',
      acceptedAnswersText: '',
    }));
  }

  const createMenuItems: NonNullable<MenuProps['items']> = useMemo(() => {
    const items: NonNullable<MenuProps['items']> = [];
    if (canCreate) {
      items.push({ key: 'create-question', label: '新增题目' });
    }
    if (canReadType && (canCreateType || canUpdateType || canDeleteType)) {
      items.push({ key: 'manage-types', label: '题型管理' });
    }
    return items;
  }, [canCreate, canCreateType, canDeleteType, canReadType, canUpdateType]);

  function handleCreateMenuClick({ key }: { key: string }) {
    if (key === 'create-question') {
      openCreateQuestionForm();
      return;
    }
    if (key === 'manage-types') {
      openTypeManager();
    }
  }

  const columns: ColumnsType<QuestionListRecord> = [
    {
      title: '题干',
      dataIndex: 'stem',
      key: 'stem',
    },
    {
      title: '题型',
      dataIndex: 'questionTypeName',
      key: 'questionTypeName',
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      key: 'difficulty',
      render: (value: QuestionDifficulty) => {
        const color = value === 'EASY' ? 'success' : value === 'MEDIUM' ? 'processing' : 'warning';
        const label = value === 'EASY' ? '简单' : value === 'MEDIUM' ? '中等' : '困难';
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '分值',
      dataIndex: 'score',
      key: 'score',
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space size="small" wrap>
          {canUpdate ? (
            <Button type="default" onClick={() => void openEditQuestionForm(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Button danger onClick={() => void handleDeleteQuestion(record)}>
              删除
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  if (!canRead) {
    return (
      <AdminPage>
        <AdminPageHeader title="题库管理" description="当前页面用于统一处理题目录入、维护与题型配置。" />
        <AdminPageSection title="题库管理">
          <Alert showIcon type="warning" message="当前账号缺少题库查询权限，请联系管理员分配权限。" />
        </AdminPageSection>
      </AdminPage>
    );
  }

  return (
    <AdminPage>
      <AdminPageHeader title="题库管理" description="按关键字、题型和难度筛选题目，并在统一工作区内完成题目与题型维护。" />

      <AdminPageSection
        title="筛选条件"
        description="优先收敛题目范围，再进入列表区执行录入、编辑和删除动作。"
        extra={
          <Space wrap>
            <Button onClick={() => void handleQuery()}>
              查询
            </Button>
          </Space>
        }
      >
        <Form layout="vertical" className={styles.filterGrid}>
          <Form.Item label="关键字">
            <Input
              aria-label="关键字"
              placeholder="请输入题干关键字"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="题型">
            <select
              aria-label="题型"
              className={styles.nativeSelect}
              value={questionTypeId}
              onChange={(event) => setQuestionTypeId(event.target.value)}
            >
              <option value="">全部</option>
              {questionTypes.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </Form.Item>
          <Form.Item label="难度">
            <select
              aria-label="难度"
              className={styles.nativeSelect}
              value={difficulty}
              onChange={(event) => setDifficulty(event.target.value)}
            >
              <option value="">全部</option>
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
            </select>
          </Form.Item>
        </Form>

        {errorMessage ? <Alert className={styles.feedback} showIcon type="error" message={errorMessage} /> : null}
      </AdminPageSection>

      <AdminPageSection
        title="题目列表"
        description="统一在列表区内处理新增、编辑、删除与题型维护。"
        extra={
          <Space wrap className={styles.toolbar}>
            {createMenuItems.length > 0 ? (
              <Dropdown menu={{ items: createMenuItems, onClick: handleCreateMenuClick }} trigger={['click']}>
                <Button type="primary">新增</Button>
              </Dropdown>
            ) : null}
            <Typography.Text type="secondary">当前结果 {records.length} 条</Typography.Text>
          </Space>
        }
      >
        <Table
          className={styles.table}
          rowKey="id"
          columns={columns}
          dataSource={records}
          loading={loading}
          pagination={false}
          scroll={{ x: 780 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={loading ? '正在加载题目数据...' : '当前筛选条件下暂无题目数据。'}
              />
            ),
          }}
        />
      </AdminPageSection>

      <Modal
        destroyOnHidden
        open={formOpen}
        title={editingRecord ? '编辑题目' : '新增题目'}
        okText={editingRecord ? '保存' : '创建'}
        cancelText="取消"
        confirmLoading={saving}
        onOk={() => void handleSaveQuestion()}
        onCancel={closeQuestionModal}
      >
        <Form layout="vertical">
          <div className={styles.formGrid}>
            <Form.Item label="题干">
              <Input.TextArea
                aria-label="题干"
                value={questionForm.stem}
                rows={4}
                onChange={(event) => setQuestionForm((current) => ({ ...current, stem: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="题型">
              <select
                aria-label="题型"
                className={styles.nativeSelect}
                value={questionForm.questionTypeId}
                onChange={(event) => handleQuestionTypeChange(event.target.value)}
              >
                <option value="">请选择题型</option>
                {questionTypes.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </Form.Item>
            <Form.Item label="难度">
              <select
                aria-label="难度"
                className={styles.nativeSelect}
                value={questionForm.difficulty}
                onChange={(event) =>
                  setQuestionForm((current) => ({ ...current, difficulty: event.target.value as QuestionDifficulty }))
                }
              >
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </Form.Item>
            <Form.Item label="分值">
              <InputNumber
                aria-label="分值"
                min={0}
                step={1}
                style={{ width: '100%' }}
                value={questionForm.score}
                onChange={(value) => setQuestionForm((current) => ({ ...current, score: Number(value ?? 0) }))}
              />
            </Form.Item>
          </div>

          <Form.Item label="答案配置">
            <div className={styles.answerBlock}>
              {(questionForm.answerMode === 'SINGLE_CHOICE' || questionForm.answerMode === 'MULTIPLE_CHOICE') ? (
                <>
                  <div className={styles.optionGrid}>
                    {questionForm.options.map((option, index) => (
                      <div key={option.key} className={styles.optionItem}>
                        <Form.Item label={`选项 ${option.key}`}>
                          <Input
                            aria-label={`选项 ${option.key}`}
                            value={option.content}
                            onChange={(event) => updateOption(index, event.target.value)}
                          />
                        </Form.Item>
                        <Button
                          danger
                          type="text"
                          aria-label={`删除选项 ${option.key}`}
                          disabled={questionForm.options.length <= MIN_CHOICE_OPTION_COUNT}
                          onClick={() => removeOption(index)}
                        >
                          删除选项 {option.key}
                        </Button>
                      </div>
                    ))}
                  </div>
                  <Button type="dashed" onClick={addOption}>
                    新增选项
                  </Button>
                  {questionForm.answerMode === 'SINGLE_CHOICE' ? (
                    <Form.Item label="正确答案">
                      <select
                        aria-label="正确答案"
                        className={styles.nativeSelect}
                        value={questionForm.correctOption}
                        onChange={(event) =>
                          setQuestionForm((current) => ({ ...current, correctOption: event.target.value }))
                        }
                      >
                        <option value="">请选择正确答案</option>
                        {questionForm.options.map((option) => (
                          <option key={option.key} value={option.key}>
                            {option.key}
                          </option>
                        ))}
                      </select>
                    </Form.Item>
                  ) : (
                    <Form.Item label="正确答案">
                      <select
                        multiple
                        aria-label="正确答案"
                        className={styles.nativeSelect}
                        value={questionForm.correctOptions}
                        onChange={(event) =>
                          setQuestionForm((current) => ({
                            ...current,
                            correctOptions: Array.from(event.target.selectedOptions).map((item) => item.value),
                          }))
                        }
                      >
                        {questionForm.options.map((option) => (
                          <option key={option.key} value={option.key}>
                            {option.key}
                          </option>
                        ))}
                      </select>
                    </Form.Item>
                  )}
                </>
              ) : null}

              {questionForm.answerMode === 'TRUE_FALSE' ? (
                <Form.Item label="正确答案">
                  <select
                    aria-label="正确答案"
                    className={styles.nativeSelect}
                    value={questionForm.correctAnswer}
                    onChange={(event) =>
                      setQuestionForm((current) => ({ ...current, correctAnswer: event.target.value as 'true' | 'false' }))
                    }
                  >
                    <option value="true">正确</option>
                    <option value="false">错误</option>
                  </select>
                </Form.Item>
              ) : null}

              {questionForm.answerMode === 'TEXT' ? (
                <Form.Item label="参考答案">
                  <Input.TextArea
                    aria-label="参考答案"
                    rows={4}
                    placeholder="每行一个参考答案"
                    value={questionForm.acceptedAnswersText}
                    onChange={(event) =>
                      setQuestionForm((current) => ({ ...current, acceptedAnswersText: event.target.value }))
                    }
                  />
                </Form.Item>
              ) : null}
            </div>
          </Form.Item>
        </Form>

        {formErrorMessage ? <Alert className={styles.feedback} showIcon type="error" message={formErrorMessage} /> : null}
      </Modal>

      <Modal
        destroyOnHidden
        open={typeManagerOpen}
        title="题型管理"
        footer={null}
        onCancel={closeTypeManager}
      >
        <div className={styles.typeManager}>
          <Form layout="vertical">
            <div className={styles.formGrid}>
              <Form.Item label="题型名称">
                <Input
                  aria-label="题型名称"
                  value={typeForm.name}
                  onChange={(event) => setTypeForm((current) => ({ ...current, name: event.target.value }))}
                />
              </Form.Item>
              <Form.Item label="答案模式">
                <select
                  aria-label="答案模式"
                  className={styles.nativeSelect}
                  value={typeForm.answerMode}
                  onChange={(event) =>
                    setTypeForm((current) => ({ ...current, answerMode: event.target.value as QuestionAnswerMode }))
                  }
                >
                  <option value="SINGLE_CHOICE">单选</option>
                  <option value="MULTIPLE_CHOICE">多选</option>
                  <option value="TRUE_FALSE">判断</option>
                  <option value="TEXT">文本</option>
                </select>
              </Form.Item>
              <Form.Item label="排序">
                <Input
                  aria-label="排序"
                  type="number"
                  value={typeForm.sort}
                  onChange={(event) => setTypeForm((current) => ({ ...current, sort: Number(event.target.value || 0) }))}
                />
              </Form.Item>
              <Form.Item label="题型备注">
                <Input
                  aria-label="题型备注"
                  value={typeForm.remark}
                  onChange={(event) => setTypeForm((current) => ({ ...current, remark: event.target.value }))}
                />
              </Form.Item>
            </div>
            <Space>
              <Button type="primary" loading={typeSaving} onClick={() => void handleSaveQuestionType()}>
                保存题型
              </Button>
              {editingType ? <Button onClick={resetTypeForm}>取消编辑</Button> : null}
            </Space>
          </Form>

          {typeErrorMessage ? <Alert showIcon type="error" message={typeErrorMessage} /> : null}

          {questionTypes.map((item) => (
            <div key={item.id} className={styles.typeCard}>
              <div className={styles.typeCardHeader}>
                <div>
                  <Typography.Text strong>{item.name}</Typography.Text>
                  <div className={styles.typeCardMeta}>
                    模式：{item.answerMode} | 排序：{item.sort}
                  </div>
                </div>
                <Space>
                  {canUpdateType ? (
                    <Button type="default" onClick={() => startEditQuestionType(item)}>
                      编辑
                    </Button>
                  ) : null}
                  {canDeleteType ? (
                    <Button danger onClick={() => void handleDeleteQuestionType(item)}>
                      删除
                    </Button>
                  ) : null}
                </Space>
              </div>
            </div>
          ))}
        </div>
      </Modal>
    </AdminPage>
  );

  function updateOption(index: number, content: string) {
    setQuestionForm((current) => ({
      ...current,
      options: current.options.map((option, currentIndex) =>
        currentIndex === index ? { ...option, content } : option,
      ),
    }));
  }

  function addOption() {
    setQuestionForm((current) => ({
      ...current,
      options: [
        ...current.options,
        {
          key: getChoiceOptionKey(current.options.length),
          content: '',
        },
      ],
    }));
  }

  function removeOption(index: number) {
    setQuestionForm((current) => {
      if (current.options.length <= MIN_CHOICE_OPTION_COUNT) {
        return current;
      }

      const removedOption = current.options[index];
      const remainingOptions = current.options.filter((_, currentIndex) => currentIndex !== index);

      return {
        ...current,
        options: remainingOptions,
        correctOption: current.correctOption === removedOption.key ? '' : current.correctOption,
        correctOptions: current.correctOptions.filter((item) => item !== removedOption.key),
      };
    });
  }
}

function fromQuestionDetail(detail: QuestionDetailRecord): QuestionFormState {
  const baseState: QuestionFormState = {
    stem: detail.stem,
    questionTypeId: String(detail.questionTypeId),
    difficulty: detail.difficulty,
    score: detail.score,
    answerMode: detail.answerMode,
    options: createChoiceOptions(),
    correctOption: '',
    correctOptions: [],
    correctAnswer: 'true',
    acceptedAnswersText: '',
  };

  if ('correctOption' in detail.answerConfig) {
    return {
      ...baseState,
      options: detail.answerConfig.options,
      correctOption: detail.answerConfig.correctOption,
    };
  }
  if ('correctOptions' in detail.answerConfig) {
    return {
      ...baseState,
      options: detail.answerConfig.options,
      correctOptions: detail.answerConfig.correctOptions,
    };
  }
  if ('correctAnswer' in detail.answerConfig) {
    return {
      ...baseState,
      correctAnswer: detail.answerConfig.correctAnswer ? 'true' : 'false',
    };
  }
  return {
    ...baseState,
    acceptedAnswersText: detail.answerConfig.acceptedAnswers.join('\n'),
  };
}

function createChoiceOptions(count = DEFAULT_CHOICE_OPTION_COUNT): ChoiceOption[] {
  return Array.from({ length: count }, (_, index) => ({
    key: getChoiceOptionKey(index),
    content: '',
  }));
}

function ensureChoiceOptions(options: ChoiceOption[]) {
  if (options.length >= DEFAULT_CHOICE_OPTION_COUNT) {
    return options;
  }

  const nextOptions = [...options];
  for (let index = options.length; index < DEFAULT_CHOICE_OPTION_COUNT; index += 1) {
    nextOptions.push({
      key: getChoiceOptionKey(index),
      content: '',
    });
  }
  return nextOptions;
}

function getChoiceOptionKey(index: number) {
  return String.fromCharCode(65 + index);
}

function buildQuestionPayload(form: QuestionFormState): QuestionPayload | null {
  if (!form.stem.trim() || !form.questionTypeId || !form.difficulty || form.score <= 0) {
    return null;
  }

  const answerConfig = buildAnswerConfig(form);
  if (!answerConfig) {
    return null;
  }

  return {
    stem: form.stem.trim(),
    questionTypeId: Number(form.questionTypeId),
    difficulty: form.difficulty,
    score: form.score,
    answerConfig,
  };
}

function buildAnswerConfig(form: QuestionFormState) {
  switch (form.answerMode) {
    case 'SINGLE_CHOICE': {
      const options = form.options.filter((item) => item.content.trim());
      if (options.length < 2 || !form.correctOption) {
        return null;
      }
      return { options, correctOption: form.correctOption } as QuestionPayload['answerConfig'];
    }
    case 'MULTIPLE_CHOICE': {
      const options = form.options.filter((item) => item.content.trim());
      if (options.length < 2 || form.correctOptions.length === 0) {
        return null;
      }
      return { options, correctOptions: form.correctOptions } as QuestionPayload['answerConfig'];
    }
    case 'TRUE_FALSE':
      return { correctAnswer: form.correctAnswer === 'true' } as QuestionPayload['answerConfig'];
    case 'TEXT': {
      const acceptedAnswers = form.acceptedAnswersText
        .split('\n')
        .map((item) => item.trim())
        .filter(Boolean);
      if (acceptedAnswers.length === 0) {
        return null;
      }
      return { acceptedAnswers } as QuestionPayload['answerConfig'];
    }
    default:
      return null;
  }
}

function buildQuestionTypePayload(form: QuestionTypeFormState): QuestionTypePayload | null {
  if (!form.name.trim() || !form.answerMode || form.sort <= 0) {
    return null;
  }

  return {
    name: form.name.trim(),
    answerMode: form.answerMode,
    sort: form.sort,
    remark: form.remark.trim(),
  };
}
