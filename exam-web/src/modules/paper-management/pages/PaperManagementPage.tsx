import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Alert, Button, Empty, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { listQuestionTypes, listQuestions } from '../../question-bank/services/questionApi';
import {
  addPaperQuestions,
  createPaper,
  deletePaper,
  deletePaperQuestion,
  getPaper,
  listPaperQuestions,
  listPapers,
  updatePaper,
  updatePaperQuestion,
} from '../services/paperApi';
import type { PaperDetailRecord, PaperListRecord, PaperPayload, PaperQuestionRecord } from '../types';
import type { QuestionListRecord } from '../../question-bank/types';
import { AdminPage, AdminPageHeader, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import { extractErrorMessage } from '../../../shared/utils/http';

type PaperManagementPageProps = {
  token: string;
  permissions: string[];
};

type PaperFormState = {
  name: string;
  description: string;
  durationMinutes: string;
  remark: string;
};

type QuestionEditorState = {
  id: number;
  itemScore: string;
  displayOrder: number;
};

const EMPTY_FORM: PaperFormState = {
  name: '',
  description: '',
  durationMinutes: '',
  remark: '',
};

export function PaperManagementPage({ token, permissions }: PaperManagementPageProps) {
  const [keyword, setKeyword] = useState('');
  const [records, setRecords] = useState<PaperListRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [formErrorMessage, setFormErrorMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [editingRecord, setEditingRecord] = useState<PaperListRecord | null>(null);
  const [paperForm, setPaperForm] = useState<PaperFormState>(EMPTY_FORM);
  const [questionDialogOpen, setQuestionDialogOpen] = useState(false);
  const [selectedPaper, setSelectedPaper] = useState<PaperDetailRecord | null>(null);
  const [paperQuestions, setPaperQuestions] = useState<PaperQuestionRecord[]>([]);
  const [questionEditors, setQuestionEditors] = useState<Record<number, QuestionEditorState>>({});
  const [questionErrorMessage, setQuestionErrorMessage] = useState('');
  const [candidateOpen, setCandidateOpen] = useState(false);
  const [candidateRecords, setCandidateRecords] = useState<QuestionListRecord[]>([]);
  const [selectedQuestionIds, setSelectedQuestionIds] = useState<number[]>([]);

  const canRead = permissions.includes('paper:read');
  const canCreate = permissions.includes('paper:create');
  const canUpdate = permissions.includes('paper:update');
  const canDelete = permissions.includes('paper:delete');
  const canReadQuestion = permissions.includes('paper-question:read');
  const canCreateQuestion = permissions.includes('paper-question:create');
  const canUpdateQuestion = permissions.includes('paper-question:update');
  const canDeleteQuestion = permissions.includes('paper-question:delete');

  useEffect(() => {
    if (!canRead) {
      setLoading(false);
      return;
    }
    void loadPapers('');
  }, [canRead, token]);

  async function loadPapers(nextKeyword: string) {
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await listPapers(token, {
        keyword: nextKeyword,
        page: 1,
        pageSize: 10,
      });
      setRecords(response.records);
    } catch {
      setErrorMessage('试卷数据加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  async function refreshQuestionDialog(paperId: number) {
    const [paper, questions] = await Promise.all([
      getPaper(token, paperId),
      listPaperQuestions(token, paperId),
    ]);
    setSelectedPaper(paper);
    setPaperQuestions(questions);
    setQuestionEditors(Object.fromEntries(
      questions.map((item) => [
        item.id,
        {
          id: item.id,
          itemScore: String(item.itemScore),
          displayOrder: item.displayOrder,
        },
      ]),
    ));
  }

  function openCreateDialog() {
    setEditingRecord(null);
    setPaperForm(EMPTY_FORM);
    setFormErrorMessage('');
    setFormOpen(true);
  }

  async function openEditDialog(record: PaperListRecord) {
    try {
      const detail = await getPaper(token, record.id);
      setEditingRecord(record);
      setPaperForm({
        name: detail.name,
        description: detail.description ?? '',
        durationMinutes: String(detail.durationMinutes),
        remark: detail.remark ?? '',
      });
      setFormErrorMessage('');
      setFormOpen(true);
    } catch {
      setErrorMessage('试卷详情加载失败，请稍后重试');
    }
  }

  async function openQuestionDialog(record: PaperListRecord) {
    try {
      await refreshQuestionDialog(record.id);
      setQuestionErrorMessage('');
      setQuestionDialogOpen(true);
    } catch {
      setErrorMessage('试卷题目明细加载失败，请稍后重试');
    }
  }

  function closeFormDialog() {
    setFormOpen(false);
    setPaperForm(EMPTY_FORM);
    setEditingRecord(null);
    setFormErrorMessage('');
    setSaving(false);
  }

  function buildPaperPayload(): PaperPayload | null {
    const durationMinutes = Number(paperForm.durationMinutes);
    if (!paperForm.name.trim() || !durationMinutes || durationMinutes <= 0) {
      return null;
    }
    return {
      name: paperForm.name.trim(),
      description: paperForm.description.trim(),
      durationMinutes,
      remark: paperForm.remark.trim(),
    };
  }

  async function handleSavePaper() {
    const payload = buildPaperPayload();
    if (!payload) {
      setFormErrorMessage('请填写试卷名称和考试时长');
      return;
    }

    setSaving(true);
    setFormErrorMessage('');
    try {
      if (editingRecord) {
        await updatePaper(token, editingRecord.id, payload);
      } else {
        await createPaper(token, payload);
      }
      closeFormDialog();
      await loadPapers(keyword);
    } catch (error) {
      setFormErrorMessage(extractErrorMessage(error, '保存试卷失败，请稍后重试'));
      setSaving(false);
    }
  }

  async function handleDeletePaper(record: PaperListRecord) {
    if (!window.confirm(`确认删除试卷“${record.name}”吗？`)) {
      return;
    }
    try {
      await deletePaper(token, record.id);
      await loadPapers(keyword);
    } catch {
      setErrorMessage('删除试卷失败，请稍后重试');
    }
  }

  async function openCandidateDialog() {
    try {
      await Promise.all([
        listQuestionTypes(token),
        listQuestions(token, {
          keyword: '',
          questionTypeId: undefined,
          difficulty: undefined,
          page: 1,
          pageSize: 10,
        }),
      ]).then(([, response]) => {
        setCandidateRecords(response.records);
      });
      setSelectedQuestionIds([]);
      setCandidateOpen(true);
    } catch {
      setQuestionErrorMessage('候选题目加载失败，请稍后重试');
    }
  }

  async function handleAddQuestions() {
    if (!selectedPaper || selectedQuestionIds.length === 0) {
      return;
    }
    try {
      await addPaperQuestions(token, selectedPaper.id, {
        questionIds: selectedQuestionIds,
      });
      setCandidateOpen(false);
      await refreshQuestionDialog(selectedPaper.id);
      await loadPapers(keyword);
    } catch {
      setQuestionErrorMessage('加入试卷失败，请稍后重试');
    }
  }

  async function handleSavePaperQuestion(paperQuestionId: number) {
    if (!selectedPaper) {
      return;
    }
    const editor = questionEditors[paperQuestionId];
    if (!editor || !editor.itemScore.trim()) {
      setQuestionErrorMessage('请填写题目分值');
      return;
    }
    try {
      await updatePaperQuestion(token, selectedPaper.id, paperQuestionId, {
        itemScore: Number(editor.itemScore),
        displayOrder: editor.displayOrder,
      });
      await refreshQuestionDialog(selectedPaper.id);
      await loadPapers(keyword);
    } catch {
      setQuestionErrorMessage('更新题目失败，请稍后重试');
    }
  }

  async function handleDeletePaperQuestion(paperQuestionId: number) {
    if (!selectedPaper) {
      return;
    }
    try {
      await deletePaperQuestion(token, selectedPaper.id, paperQuestionId);
      await refreshQuestionDialog(selectedPaper.id);
      await loadPapers(keyword);
    } catch {
      setQuestionErrorMessage('移除题目失败，请稍后重试');
    }
  }

  const columns: ColumnsType<PaperListRecord> = [
    { title: '试卷名称', dataIndex: 'name', key: 'name' },
    { title: '题目数量', dataIndex: 'questionCount', key: 'questionCount' },
    { title: '总分', dataIndex: 'totalScore', key: 'totalScore' },
    { title: '时长（分钟）', dataIndex: 'durationMinutes', key: 'durationMinutes' },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          {canUpdate ? <Button onClick={() => void openEditDialog(record)}>编辑基础信息</Button> : null}
          {canReadQuestion ? <Button onClick={() => void openQuestionDialog(record)}>维护题目</Button> : null}
          {canDelete ? <Button danger onClick={() => void handleDeletePaper(record)}>删除试卷</Button> : null}
        </Space>
      ),
    },
  ];

  if (!canRead) {
    return (
      <AdminPage>
        <AdminPageHeader
          title="试卷管理"
          description="当前账号没有试卷读取权限。"
        />
        <Empty description="暂无试卷权限" />
      </AdminPage>
    );
  }

  return (
    <AdminPage>
      <AdminPageHeader
        title="试卷管理"
        description="基于题库组织试卷，维护试卷基础信息、题目明细、总分与时长配置。"
        extra={canCreate ? <Button type="primary" onClick={openCreateDialog}>新增试卷</Button> : null}
      />

      <AdminPageSection title="试卷列表" description="支持按关键字查询现有试卷。">
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span>关键字</span>
            <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
          </label>
          <Button onClick={() => void loadPapers(keyword)}>查询</Button>
        </div>

        {errorMessage ? <Alert type="error" message={errorMessage} showIcon style={{ marginBottom: 16 }} /> : null}
        <Table rowKey="id" loading={loading} columns={columns} dataSource={records} pagination={false} locale={{ emptyText: '暂无试卷数据' }} />
      </AdminPageSection>

      {formOpen ? (
        <div role="dialog" aria-label={editingRecord ? '编辑试卷' : '新增试卷'} style={dialogStyle}>
          <h3>{editingRecord ? '编辑试卷' : '新增试卷'}</h3>
          <div style={dialogBodyStyle}>
            <label style={fieldStyle}>
              <span>试卷名称</span>
              <input
                value={paperForm.name}
                onChange={(event) => setPaperForm((current) => ({ ...current, name: event.target.value }))}
              />
            </label>
            <label style={fieldStyle}>
              <span>试卷说明</span>
              <textarea
                value={paperForm.description}
                onChange={(event) => setPaperForm((current) => ({ ...current, description: event.target.value }))}
              />
            </label>
            <label style={fieldStyle}>
              <span>考试时长（分钟）</span>
              <input
                type="number"
                value={paperForm.durationMinutes}
                onChange={(event) => setPaperForm((current) => ({ ...current, durationMinutes: event.target.value }))}
              />
            </label>
            <label style={fieldStyle}>
              <span>试卷备注</span>
              <textarea
                value={paperForm.remark}
                onChange={(event) => setPaperForm((current) => ({ ...current, remark: event.target.value }))}
              />
            </label>
          </div>
          {formErrorMessage ? <div role="alert">{formErrorMessage}</div> : null}
          <div style={dialogFooterStyle}>
            <Button onClick={closeFormDialog}>取消</Button>
            <Button type="primary" loading={saving} onClick={() => void handleSavePaper()}>
              {editingRecord ? '保存试卷' : '创建试卷'}
            </Button>
          </div>
        </div>
      ) : null}

      {questionDialogOpen && selectedPaper ? (
        <div role="dialog" aria-label="维护试卷题目" style={questionDialogStyle}>
          <h3>维护试卷题目</h3>
          <p>{`当前总分：${Number(selectedPaper.totalScore)}`}</p>
          {questionErrorMessage ? <div role="alert">{questionErrorMessage}</div> : null}
          {canCreateQuestion ? <Button onClick={() => void openCandidateDialog()}>手工组卷</Button> : null}
          <div style={{ marginTop: 16, display: 'grid', gap: 12 }}>
            {paperQuestions.map((item) => (
              <div key={item.id} style={questionCardStyle}>
                <div>{item.questionStemSnapshot}</div>
                <div>{item.questionTypeNameSnapshot}</div>
                <label style={fieldStyle}>
                  <span>{`题目 ${item.id} 分值`}</span>
                  <input
                    aria-label={`题目 ${item.id} 分值`}
                    type="number"
                    value={questionEditors[item.id]?.itemScore ?? ''}
                    onChange={(event) => setQuestionEditors((current) => ({
                      ...current,
                      [item.id]: {
                        ...current[item.id],
                        id: item.id,
                        itemScore: event.target.value,
                        displayOrder: current[item.id]?.displayOrder ?? item.displayOrder,
                      },
                    }))}
                  />
                </label>
                <div style={{ display: 'flex', gap: 8 }}>
                  {canUpdateQuestion ? <Button onClick={() => void handleSavePaperQuestion(item.id)}>{`保存题目 ${item.id}`}</Button> : null}
                  {canDeleteQuestion ? <Button danger onClick={() => void handleDeletePaperQuestion(item.id)}>{`移除题目 ${item.id}`}</Button> : null}
                </div>
              </div>
            ))}
          </div>
          <div style={dialogFooterStyle}>
            <Button onClick={() => setQuestionDialogOpen(false)}>关闭</Button>
          </div>
        </div>
      ) : null}

      {candidateOpen ? (
        <div role="dialog" aria-label="手工组选题" style={dialogStyle}>
          <h3>手工组选题</h3>
          <div style={{ display: 'grid', gap: 12 }}>
            {candidateRecords.map((record) => (
              <label key={record.id} style={candidateItemStyle}>
                <input
                  aria-label={`选择题目 ${record.id}`}
                  type="checkbox"
                  checked={selectedQuestionIds.includes(record.id)}
                  onChange={(event) => {
                    setSelectedQuestionIds((current) => (
                      event.target.checked
                        ? [...current, record.id]
                        : current.filter((item) => item !== record.id)
                    ));
                  }}
                />
                <span>{record.stem}</span>
              </label>
            ))}
          </div>
          <div style={dialogFooterStyle}>
            <Button onClick={() => setCandidateOpen(false)}>取消</Button>
            <Button type="primary" onClick={() => void handleAddQuestions()}>加入试卷</Button>
          </div>
        </div>
      ) : null}
    </AdminPage>
  );
}

const dialogStyle: CSSProperties = {
  position: 'fixed',
  top: '10%',
  left: '50%',
  transform: 'translateX(-50%)',
  width: 'min(720px, 90vw)',
  background: '#fff',
  border: '1px solid #d9d9d9',
  borderRadius: 12,
  padding: 20,
  zIndex: 1000,
  boxShadow: '0 12px 32px rgba(0, 0, 0, 0.12)',
};

const questionDialogStyle: CSSProperties = {
  ...dialogStyle,
  maxHeight: '80vh',
  overflowY: 'auto',
};

const dialogBodyStyle: CSSProperties = {
  display: 'grid',
  gap: 12,
  marginBottom: 16,
};

const dialogFooterStyle: CSSProperties = {
  display: 'flex',
  justifyContent: 'flex-end',
  gap: 8,
  marginTop: 16,
};

const fieldStyle: CSSProperties = {
  display: 'grid',
  gap: 4,
};

const questionCardStyle: CSSProperties = {
  display: 'grid',
  gap: 8,
  padding: 12,
  border: '1px solid #e5e7eb',
  borderRadius: 8,
};

const candidateItemStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
};
