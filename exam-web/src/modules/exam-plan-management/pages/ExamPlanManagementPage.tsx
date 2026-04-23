import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import { Alert, Button, DatePicker, Empty, Form, Input, Modal, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { listExaminees } from '../../examinees/services/examineeApi';
import { listPapers } from '../../paper-management/services/paperApi';
import type { ExamineeRecord } from '../../examinees/types';
import type { PaperListRecord } from '../../paper-management/types';
import { AdminPage, AdminPageHeader, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import { extractErrorMessage } from '../../../shared/utils/http';
import {
  createExamPlan,
  getExamPlan,
  listExamPlanExaminees,
  listExamPlans,
  updateExamPlan,
  updateExamPlanExaminees,
  updateExamPlanStatus,
} from '../services/examPlanApi';
import type {
  ExamPlanDetailRecord,
  ExamPlanListRecord,
  ExamPlanPayload,
  ExamPlanStatus,
} from '../types';

type ExamPlanManagementPageProps = {
  token: string;
  permissions: string[];
};

type QueryState = {
  keyword: string;
  status: '' | ExamPlanStatus;
};

type PlanFormState = {
  name: string;
  paperId: string;
  startTime: string;
  endTime: string;
  remark: string;
};

const EMPTY_QUERY: QueryState = {
  keyword: '',
  status: '',
};

const EMPTY_FORM: PlanFormState = {
  name: '',
  paperId: '',
  startTime: '',
  endTime: '',
  remark: '',
};

const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm';
const API_DATE_TIME_FORMAT = 'YYYY-MM-DDTHH:mm:ss';
const STATUS_LABELS: Record<ExamPlanStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
};

export function ExamPlanManagementPage({ token, permissions }: ExamPlanManagementPageProps) {
  const [query, setQuery] = useState<QueryState>(EMPTY_QUERY);
  const [records, setRecords] = useState<ExamPlanListRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [papers, setPapers] = useState<PaperListRecord[]>([]);
  const [planDialogOpen, setPlanDialogOpen] = useState(false);
  const [planDialogTitle, setPlanDialogTitle] = useState('新增考试计划');
  const [planForm, setPlanForm] = useState<PlanFormState>(EMPTY_FORM);
  const [planFormError, setPlanFormError] = useState('');
  const [editingPlanId, setEditingPlanId] = useState<number | null>(null);

  const [rangeDialogOpen, setRangeDialogOpen] = useState(false);
  const [rangePlanId, setRangePlanId] = useState<number | null>(null);
  const [rangeErrorMessage, setRangeErrorMessage] = useState('');
  const [candidateExaminees, setCandidateExaminees] = useState<ExamineeRecord[]>([]);
  const [selectedExamineeIds, setSelectedExamineeIds] = useState<number[]>([]);

  const canRead = permissions.includes('exam-plan:read');
  const canCreate = permissions.includes('exam-plan:create');
  const canUpdate = permissions.includes('exam-plan:update');
  const canRange = permissions.includes('exam-plan:range');
  const canStatus = permissions.includes('exam-plan:status');

  useEffect(() => {
    if (!canRead) {
      setLoading(false);
      return;
    }
    void Promise.all([
      loadExamPlans(EMPTY_QUERY),
      loadPaperOptions(),
    ]);
  }, [canRead, token]);

  async function loadExamPlans(nextQuery: QueryState) {
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await listExamPlans(token, {
        keyword: nextQuery.keyword,
        status: nextQuery.status,
        page: 1,
        pageSize: 10,
      });
      setRecords(response.records);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '考试计划加载失败，请稍后重试'));
    } finally {
      setLoading(false);
    }
  }

  async function loadPaperOptions() {
    try {
      const response = await listPapers(token, {
        keyword: '',
        page: 1,
        pageSize: 50,
      });
      setPapers(response.records);
    } catch {
      setPapers([]);
    }
  }

  function buildPayload(form: PlanFormState): ExamPlanPayload | null {
    if (!form.name.trim() || !form.paperId || !form.startTime || !form.endTime) {
      return null;
    }

    return {
      name: form.name.trim(),
      paperId: Number(form.paperId),
      startTime: form.startTime,
      endTime: form.endTime,
      remark: form.remark.trim(),
    };
  }

  function getPickerValue(value: string) {
    if (!value) {
      return null;
    }

    const parsed = dayjs(value);
    return parsed.isValid() ? parsed : null;
  }

  function formatDateTime(value: string) {
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format(DATE_TIME_FORMAT) : value;
  }

  function hydratePlanForm(record: Pick<ExamPlanDetailRecord, 'name' | 'paperId' | 'startTime' | 'endTime' | 'remark'>) {
    setPlanForm({
      name: record.name,
      paperId: String(record.paperId),
      startTime: record.startTime,
      endTime: record.endTime,
      remark: record.remark ?? '',
    });
  }

  function closePlanDialog() {
    setPlanDialogOpen(false);
    setPlanDialogTitle('新增考试计划');
    setEditingPlanId(null);
    setPlanForm(EMPTY_FORM);
    setPlanFormError('');
  }

  function closeRangeDialog() {
    setRangeDialogOpen(false);
    setRangePlanId(null);
    setRangeErrorMessage('');
    setCandidateExaminees([]);
    setSelectedExamineeIds([]);
  }

  function openCreateDialog() {
    setPlanDialogTitle('新增考试计划');
    setEditingPlanId(null);
    setPlanForm(EMPTY_FORM);
    setPlanFormError('');
    setPlanDialogOpen(true);
  }

  async function openEditDialog(record: ExamPlanListRecord) {
    try {
      const detail = await getExamPlan(token, record.id);
      setPlanDialogTitle('编辑考试计划');
      setEditingPlanId(record.id);
      hydratePlanForm(detail);
      setPlanFormError('');
      setPlanDialogOpen(true);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '考试计划详情加载失败，请稍后重试'));
    }
  }

  async function handleSavePlan() {
    const payload = buildPayload(planForm);
    if (!payload) {
      setPlanFormError('请填写计划名称、试卷和考试时间');
      return;
    }

    setPlanFormError('');
    try {
      if (editingPlanId) {
        await updateExamPlan(token, editingPlanId, payload);
      } else {
        await createExamPlan(token, payload);
      }
      closePlanDialog();
      await loadExamPlans(query);
    } catch (error) {
      setPlanFormError(extractErrorMessage(error, '保存考试计划失败，请稍后重试'));
    }
  }

  async function openRangeDialog(record: ExamPlanListRecord) {
    try {
      const [selectedResponse, examineeResponse] = await Promise.all([
        listExamPlanExaminees(token, record.id),
        listExaminees(token, {
          keyword: '',
          status: 'ENABLED',
          page: 1,
          pageSize: 50,
        }),
      ]);

      setRangePlanId(record.id);
      setCandidateExaminees(examineeResponse.records);
      setSelectedExamineeIds(selectedResponse.map((item) => item.id));
      setRangeErrorMessage('');
      setRangeDialogOpen(true);
    } catch (error) {
      setErrorMessage(extractErrorMessage(error, '考试范围加载失败，请稍后重试'));
    }
  }

  function toggleExaminee(id: number, checked: boolean) {
    setSelectedExamineeIds((current) => {
      if (checked) {
        return current.includes(id) ? current : [...current, id];
      }
      return current.filter((item) => item !== id);
    });
  }

  async function handleSaveRange() {
    if (!rangePlanId) {
      return;
    }

    try {
      await updateExamPlanExaminees(token, rangePlanId, {
        examineeIds: selectedExamineeIds,
      });
      closeRangeDialog();
      await loadExamPlans(query);
    } catch (error) {
      setRangeErrorMessage(extractErrorMessage(error, '保存考试范围失败，请稍后重试'));
    }
  }

  async function handleStatusChange(record: ExamPlanListRecord, status: ExamPlanStatus) {
    try {
      await updateExamPlanStatus(token, record.id, status);
      await loadExamPlans(query);
    } catch (error) {
      const fallback = status === 'PUBLISHED'
        ? '发布考试失败，请稍后重试'
        : status === 'CLOSED'
          ? '关闭考试失败，请稍后重试'
          : '取消考试失败，请稍后重试';
      setErrorMessage(extractErrorMessage(error, fallback));
    }
  }

  function renderStatusActions(record: ExamPlanListRecord) {
    if (!canStatus) {
      return null;
    }

    if (record.status === 'DRAFT') {
      return (
        <>
          <Button type="primary" onClick={() => void handleStatusChange(record, 'PUBLISHED')}>发布考试</Button>
          <Button onClick={() => void handleStatusChange(record, 'CANCELLED')}>取消考试</Button>
        </>
      );
    }

    if (record.status === 'PUBLISHED') {
      return (
        <>
          <Button onClick={() => void handleStatusChange(record, 'CLOSED')}>关闭考试</Button>
          <Button danger onClick={() => void handleStatusChange(record, 'CANCELLED')}>取消考试</Button>
        </>
      );
    }

    return null;
  }

  const columns: ColumnsType<ExamPlanListRecord> = [
    { title: '计划名称', dataIndex: 'name', key: 'name' },
    { title: '试卷', dataIndex: 'paperName', key: 'paperName' },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', render: (value: string) => formatDateTime(value) },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime', render: (value: string) => formatDateTime(value) },
    { title: '考生数', dataIndex: 'effectiveExamineeCount', key: 'effectiveExamineeCount' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value: ExamPlanStatus) => STATUS_LABELS[value] ?? value },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          {canUpdate ? <Button onClick={() => void openEditDialog(record)}>编辑计划</Button> : null}
          {canRange ? <Button onClick={() => void openRangeDialog(record)}>配置范围</Button> : null}
          {renderStatusActions(record)}
        </Space>
      ),
    },
  ];

  const selectedExaminees = candidateExaminees.filter((item) => selectedExamineeIds.includes(item.id));

  if (!canRead) {
    return (
      <AdminPage>
        <AdminPageHeader title="考试计划" description="当前账号没有考试计划读取权限。" />
        <AdminPageSection title="考试计划">
          <Empty description="暂无考试计划权限" />
        </AdminPageSection>
      </AdminPage>
    );
  }

  return (
    <AdminPage>
      <AdminPageHeader
        title="考试计划"
        description="在统一工作区安排考试时间、关联试卷、配置考生范围，并维护考试状态。"
        extra={canCreate ? <Button type="primary" onClick={openCreateDialog}>新增考试</Button> : null}
      />

      <AdminPageSection title="计划列表" description="支持按关键字和状态筛选现有考试计划。">
        <div style={{ display: 'flex', gap: 12, alignItems: 'end', marginBottom: 16 }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span>关键字</span>
            <input
              aria-label="关键字"
              value={query.keyword}
              onChange={(event) => setQuery((current) => ({ ...current, keyword: event.target.value }))}
            />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span>状态</span>
            <select
              aria-label="状态"
              value={query.status}
              onChange={(event) => setQuery((current) => ({ ...current, status: event.target.value as QueryState['status'] }))}
            >
              <option value="">全部</option>
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">已发布</option>
              <option value="CLOSED">已关闭</option>
              <option value="CANCELLED">已取消</option>
            </select>
          </label>
          <Button onClick={() => void loadExamPlans(query)}>查询</Button>
        </div>

        {errorMessage ? <div role="alert">{errorMessage}</div> : null}
        <Table rowKey="id" columns={columns} dataSource={records} loading={loading} pagination={false} />
      </AdminPageSection>

      <Modal
        destroyOnHidden
        open={planDialogOpen}
        title={planDialogTitle}
        okText="保存计划"
        cancelText="取消"
        getContainer={false}
        onOk={() => void handleSavePlan()}
        onCancel={closePlanDialog}
      >
        <Form layout="vertical">
          <Form.Item label="计划名称">
            <Input
              aria-label="计划名称"
              value={planForm.name}
              onChange={(event) => setPlanForm((current) => ({ ...current, name: event.target.value }))}
            />
          </Form.Item>
          <Form.Item label="试卷">
            <select
              aria-label="试卷"
              value={planForm.paperId}
              onChange={(event) => setPlanForm((current) => ({ ...current, paperId: event.target.value }))}
            >
              <option value="">请选择试卷</option>
              {papers.map((paper) => (
                <option key={paper.id} value={paper.id}>{paper.name}</option>
              ))}
            </select>
          </Form.Item>
          <Form.Item label="开始时间">
            <DatePicker
              showTime={{ format: 'HH:mm' }}
              format={DATE_TIME_FORMAT}
              aria-label="开始时间"
              style={{ width: '100%' }}
              value={getPickerValue(planForm.startTime)}
              onChange={(value) =>
                setPlanForm((current) => ({
                  ...current,
                  startTime: value ? value.format(API_DATE_TIME_FORMAT) : '',
                }))
              }
            />
          </Form.Item>
          <Form.Item label="结束时间">
            <DatePicker
              showTime={{ format: 'HH:mm' }}
              format={DATE_TIME_FORMAT}
              aria-label="结束时间"
              style={{ width: '100%' }}
              value={getPickerValue(planForm.endTime)}
              onChange={(value) =>
                setPlanForm((current) => ({
                  ...current,
                  endTime: value ? value.format(API_DATE_TIME_FORMAT) : '',
                }))
              }
            />
          </Form.Item>
          <Form.Item label="备注">
            <Input.TextArea
              aria-label="备注"
              rows={4}
              value={planForm.remark}
              onChange={(event) => setPlanForm((current) => ({ ...current, remark: event.target.value }))}
            />
          </Form.Item>
        </Form>
        {planFormError ? <Alert showIcon type="error" message={planFormError} role="alert" /> : null}
      </Modal>

      <Modal
        destroyOnHidden
        open={rangeDialogOpen}
        title="配置考试范围"
        okText="保存范围"
        cancelText="取消"
        getContainer={false}
        onOk={() => void handleSaveRange()}
        onCancel={closeRangeDialog}
      >
        <div style={{ display: 'grid', gap: 12 }}>
          <div>{`已配置 ${selectedExamineeIds.length} 人`}</div>
          <div style={{ display: 'grid', gap: 6 }}>
            {selectedExaminees.map((record) => (
              <span key={record.id}>{`${record.name}（${record.examineeNo}）`}</span>
            ))}
          </div>
          {rangeErrorMessage ? <Alert showIcon type="error" message={rangeErrorMessage} role="alert" /> : null}
          <div style={{ display: 'grid', gap: 8 }}>
            {candidateExaminees.map((record) => (
              <label key={record.id} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input
                  type="checkbox"
                  aria-label={`选择考生 ${record.id}`}
                  checked={selectedExamineeIds.includes(record.id)}
                  onChange={(event) => toggleExaminee(record.id, event.target.checked)}
                />
                <span>{`${record.name}（${record.examineeNo}）`}</span>
              </label>
            ))}
          </div>
        </div>
      </Modal>
    </AdminPage>
  );
}
