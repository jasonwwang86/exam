import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Dropdown,
  Empty,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { ExamineeFormPayload, ExamineeRecord, ExamineeStatus, UpdateExamineePayload } from '../types';
import {
  createExaminee,
  deleteExaminee,
  exportExaminees,
  importExaminees,
  listExaminees,
  updateExaminee,
  updateExamineeStatus,
} from '../services/examineeApi';
import { AdminPage, AdminPageSection } from '../../../shared/components/admin-page/AdminPage';
import styles from './ExamineeManagementPage.module.css';

type ExamineeManagementPageProps = {
  token: string;
  permissions: string[];
};

const EMPTY_FORM: ExamineeFormPayload = {
  examineeNo: '',
  name: '',
  gender: 'MALE',
  idCardNo: '',
  phone: '',
  email: '',
  status: 'ENABLED',
  remark: '',
};

export function ExamineeManagementPage({ token, permissions }: ExamineeManagementPageProps) {
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [records, setRecords] = useState<ExamineeRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [formErrorMessage, setFormErrorMessage] = useState('');
  const [importMessage, setImportMessage] = useState('');
  const [importErrorMessage, setImportErrorMessage] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ExamineeRecord | null>(null);
  const [form, setForm] = useState<ExamineeFormPayload>(EMPTY_FORM);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const canRead = permissions.includes('examinee:read');
  const canCreate = permissions.includes('examinee:create');
  const canUpdate = permissions.includes('examinee:update');
  const canDelete = permissions.includes('examinee:delete');
  const canToggleStatus = permissions.includes('examinee:status');
  const canImport = permissions.includes('examinee:import');
  const canExport = permissions.includes('examinee:export');

  useEffect(() => {
    if (!canRead) {
      setLoading(false);
      return;
    }
    void loadExaminees({ keyword: '', status: '' });
  }, [canRead, token]);

  async function loadExaminees(nextFilters: { keyword: string; status: string }) {
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await listExaminees(token, {
        keyword: nextFilters.keyword,
        status: nextFilters.status,
        page: 1,
        pageSize: 10,
      });
      setRecords(response.records);
    } catch {
      setErrorMessage('考生数据加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }

  function resetFormState() {
    setForm(EMPTY_FORM);
    setEditingRecord(null);
    setFormErrorMessage('');
    setSaving(false);
  }

  function openCreateForm() {
    resetFormState();
    setFormOpen(true);
  }

  function openEditForm(record: ExamineeRecord) {
    setEditingRecord(record);
    setForm({
      examineeNo: record.examineeNo,
      name: record.name,
      gender: record.gender,
      idCardNo: record.idCardNo,
      phone: record.phone,
      email: record.email,
      status: record.status,
      remark: record.remark,
    });
    setFormErrorMessage('');
    setFormOpen(true);
  }

  function closeFormModal() {
    setFormOpen(false);
    resetFormState();
  }

  function openImportModal() {
    setImportErrorMessage('');
    setSelectedFile(null);
    setImportOpen(true);
  }

  function closeImportModal() {
    setImportOpen(false);
    setSelectedFile(null);
    setImportErrorMessage('');
    setImporting(false);
  }

  async function handleQuery() {
    await loadExaminees({ keyword, status });
  }

  async function handleSave() {
    if (!form.examineeNo.trim() || !form.name.trim() || !form.gender || !form.idCardNo.trim() || !form.phone.trim() || !form.status) {
      setFormErrorMessage('请填写考生编号、姓名、性别、身份证号、手机号和状态');
      return;
    }

    setFormErrorMessage('');
    setSaving(true);
    try {
      if (editingRecord) {
        const payload: UpdateExamineePayload = {
          name: form.name,
          gender: form.gender,
          idCardNo: form.idCardNo,
          phone: form.phone,
          email: form.email,
          status: form.status,
          remark: form.remark,
        };
        await updateExaminee(token, editingRecord.id, payload);
      } else {
        await createExaminee(token, form);
      }
      closeFormModal();
      await loadExaminees({ keyword, status });
    } catch {
      setFormErrorMessage('保存考生失败，请稍后重试');
      setSaving(false);
    }
  }

  async function handleDelete(record: ExamineeRecord) {
    if (!window.confirm(`确认删除考生 ${record.name} 吗？`)) {
      return;
    }

    try {
      await deleteExaminee(token, record.id);
      await loadExaminees({ keyword, status });
    } catch {
      setErrorMessage('删除考生失败，请稍后重试');
    }
  }

  async function handleToggleStatus(record: ExamineeRecord) {
    const nextStatus: ExamineeStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    try {
      await updateExamineeStatus(token, record.id, nextStatus);
      await loadExaminees({ keyword, status });
    } catch {
      setErrorMessage('更新考生状态失败，请稍后重试');
    }
  }

  async function handleImport() {
    if (!selectedFile) {
      setImportErrorMessage('请先选择导入文件');
      return;
    }

    setImportErrorMessage('');
    setImporting(true);
    try {
      const result = await importExaminees(token, selectedFile);
      setImportMessage(`导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`);
      closeImportModal();
      await loadExaminees({ keyword, status });
    } catch {
      setImportErrorMessage('导入失败，请稍后重试');
      setImporting(false);
    }
  }

  async function handleExport() {
    try {
      const blob = await exportExaminees(token, {
        keyword,
        status,
      });
      if (window.navigator.userAgent.includes('jsdom')) {
        return;
      }
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'examinees.xlsx';
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setErrorMessage('导出失败，请稍后重试');
    }
  }

  const createMenuItems: NonNullable<MenuProps['items']> = useMemo(() => {
    const items: NonNullable<MenuProps['items']> = [];
    if (canCreate) {
      items.push({
        key: 'create',
        label: '新增考生',
      });
    }
    if (canImport) {
      items.push({
        key: 'import',
        label: '导入文件',
      });
    }
    return items;
  }, [canCreate, canImport]);

  function handleCreateMenuClick({ key }: { key: string }) {
    if (key === 'create') {
      openCreateForm();
      return;
    }
    if (key === 'import') {
      openImportModal();
    }
  }

  const columns: ColumnsType<ExamineeRecord> = [
    {
      title: '考生编号',
      dataIndex: 'examineeNo',
      key: 'examineeNo',
    },
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: ExamineeRecord['status']) =>
        value === 'ENABLED' ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space size="small" wrap>
          {canUpdate ? (
            <Button type="default" onClick={() => openEditForm(record)}>
              编辑
            </Button>
          ) : null}
          {canToggleStatus ? (
            <Button type="default" onClick={() => void handleToggleStatus(record)}>
              {record.status === 'ENABLED' ? '禁用' : '启用'}
            </Button>
          ) : null}
          {canDelete ? (
            <Button danger onClick={() => void handleDelete(record)}>
              删除
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const importMessageType = importMessage.startsWith('导入完成') ? 'success' : 'warning';

  if (!canRead) {
    return (
      <AdminPage>
        <AdminPageSection title="考生管理">
          <Alert showIcon type="warning" message="当前账号缺少考生查询权限，请联系管理员分配权限。" />
        </AdminPageSection>
      </AdminPage>
    );
  }

  return (
    <AdminPage>
      <AdminPageSection
        title="考生管理"
        extra={
          <Space wrap>
            <Button onClick={() => void handleQuery()}>
              查询
            </Button>
            {createMenuItems.length > 0 ? (
              <Dropdown menu={{ items: createMenuItems, onClick: handleCreateMenuClick }} trigger={['click']}>
                <Button type="primary">新增</Button>
              </Dropdown>
            ) : null}
            {canExport ? (
              <Button onClick={() => void handleExport()}>
                导出当前结果
              </Button>
            ) : null}
          </Space>
        }
      >
        <div className={styles.filters}>
          <Form layout="vertical" className={styles.filterGrid}>
            <Form.Item label="关键字">
              <Input
                id="examinee-keyword"
                aria-label="关键字"
                placeholder="请输入考生姓名或编号"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
            </Form.Item>
            <Form.Item label="状态">
              <select
                id="examinee-status"
                aria-label="状态"
                className={styles.nativeSelect}
                value={status}
                onChange={(event) => setStatus(event.target.value)}
              >
                <option value="">全部</option>
                <option value="ENABLED">启用</option>
                <option value="DISABLED">禁用</option>
              </select>
            </Form.Item>
          </Form>
        </div>

        {importMessage ? <Alert className={styles.feedback} showIcon type={importMessageType} message={importMessage} /> : null}
        {errorMessage ? <Alert className={styles.feedback} showIcon type="error" message={errorMessage} /> : null}
      </AdminPageSection>

      <AdminPageSection
        title="考生列表"
        extra={<Typography.Text type="secondary">当前结果 {records.length} 条</Typography.Text>}
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={records}
          loading={loading}
          pagination={false}
          scroll={{ x: 720 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={loading ? '正在加载考生数据...' : '当前筛选条件下暂无考生数据。'}
              />
            ),
          }}
        />
      </AdminPageSection>

      <Modal
        destroyOnHidden
        open={formOpen}
        title={editingRecord ? '编辑考生' : '新增考生'}
        okText={editingRecord ? '保存' : '创建'}
        cancelText="取消"
        confirmLoading={saving}
        onOk={() => void handleSave()}
        onCancel={closeFormModal}
      >
        <Form layout="vertical">
          <div className={styles.formGrid}>
            <Form.Item label="考生编号">
              <Input
                id="examinee-no"
                aria-label="考生编号"
                value={form.examineeNo}
                disabled={Boolean(editingRecord)}
                onChange={(event) => setForm((current) => ({ ...current, examineeNo: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="姓名">
              <Input
                id="examinee-name"
                aria-label="姓名"
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="性别">
              <Select
                id="examinee-gender"
                aria-label="性别"
                value={form.gender}
                options={[
                  { value: 'MALE', label: '男' },
                  { value: 'FEMALE', label: '女' },
                ]}
                onChange={(value) => setForm((current) => ({ ...current, gender: value as ExamineeFormPayload['gender'] }))}
              />
            </Form.Item>
            <Form.Item label="身份证号">
              <Input
                id="examinee-id-card"
                aria-label="身份证号"
                value={form.idCardNo}
                onChange={(event) => setForm((current) => ({ ...current, idCardNo: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="手机号">
              <Input
                id="examinee-phone"
                aria-label="手机号"
                value={form.phone}
                onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="邮箱">
              <Input
                id="examinee-email"
                aria-label="邮箱"
                value={form.email}
                onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              />
            </Form.Item>
            <Form.Item label="状态">
              <Select
                id="examinee-form-status"
                aria-label="状态"
                value={form.status}
                options={[
                  { value: 'ENABLED', label: '启用' },
                  { value: 'DISABLED', label: '禁用' },
                ]}
                onChange={(value) => setForm((current) => ({ ...current, status: value as ExamineeFormPayload['status'] }))}
              />
            </Form.Item>
          </div>
          <Form.Item label="备注">
            <Input.TextArea
              id="examinee-remark"
              aria-label="备注"
              value={form.remark}
              rows={4}
              onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))}
            />
          </Form.Item>
        </Form>
        {formErrorMessage ? <Alert showIcon type="error" message={formErrorMessage} role="alert" /> : null}
      </Modal>

      <Modal
        destroyOnHidden
        open={importOpen}
        title="导入考生文件"
        okText="开始导入"
        cancelText="取消"
        confirmLoading={importing}
        onOk={() => void handleImport()}
        onCancel={closeImportModal}
      >
        <div className={styles.importPanel}>
          <div className={styles.importPanelBox}>
            <label className={styles.fileLabel} htmlFor="examinee-import-file">
              导入文件
            </label>
            <input
              id="examinee-import-file"
              className={styles.fileInput}
              aria-label="导入文件"
              type="file"
              accept=".xlsx"
              onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
            />
            <Typography.Text type="secondary">
              {selectedFile ? `已选择：${selectedFile.name}` : '请选择 Excel 文件后提交导入。'}
            </Typography.Text>
          </div>
        </div>
        {importErrorMessage ? <Alert className={styles.modalFeedback} showIcon type="warning" message={importErrorMessage} /> : null}
      </Modal>
    </AdminPage>
  );
}
