import { useEffect, useState } from 'react';
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
  const [formOpen, setFormOpen] = useState(false);
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

  function openCreateForm() {
    setEditingRecord(null);
    setForm(EMPTY_FORM);
    setFormErrorMessage('');
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

  async function handleQuery() {
    await loadExaminees({ keyword, status });
  }

  async function handleSave() {
    if (!form.examineeNo.trim() || !form.name.trim() || !form.gender || !form.idCardNo.trim() || !form.phone.trim() || !form.status) {
      setFormErrorMessage('请填写考生编号、姓名、性别、身份证号、手机号和状态');
      return;
    }

    setFormErrorMessage('');
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
      setFormOpen(false);
      setForm(EMPTY_FORM);
      await loadExaminees({ keyword, status });
    } catch {
      setFormErrorMessage('保存考生失败，请稍后重试');
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
      setImportMessage('请先选择导入文件');
      return;
    }

    try {
      const result = await importExaminees(token, selectedFile);
      setImportMessage(`导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`);
      await loadExaminees({ keyword, status });
    } catch {
      setImportMessage('导入失败，请稍后重试');
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

  if (!canRead) {
    return (
      <section className={styles.page}>
        <div className={styles.hero}>
          <h1 className={styles.title}>考生管理</h1>
          <p className={styles.description}>当前账号缺少考生查询权限，请联系管理员分配权限。</p>
        </div>
      </section>
    );
  }

  return (
    <section className={styles.page}>
      <div className={styles.hero}>
        <h1 className={styles.title}>考生管理</h1>
        <p className={styles.description}>在统一主页面下维护考生基础信息、状态与批量导入导出，后续其他管理模块也沿用同一导航结构接入。</p>
      </div>

      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="examinee-keyword">
              关键字
            </label>
            <input
              id="examinee-keyword"
              className={styles.input}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="examinee-status">
              状态
            </label>
            <select
              id="examinee-status"
              className={styles.select}
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="">全部</option>
              <option value="ENABLED">启用</option>
              <option value="DISABLED">禁用</option>
            </select>
          </div>
        </div>
        <div className={styles.actions}>
          <button className={styles.secondaryButton} type="button" onClick={() => void handleQuery()}>
            查询
          </button>
          {canCreate ? (
            <button className={styles.primaryButton} type="button" onClick={openCreateForm}>
              新增考生
            </button>
          ) : null}
          {canExport ? (
            <button className={styles.secondaryButton} type="button" onClick={() => void handleExport()}>
              导出当前结果
            </button>
          ) : null}
        </div>
        {canImport ? (
          <div className={styles.importActions}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="examinee-import-file">
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
            </div>
            <button className={styles.secondaryButton} type="button" onClick={() => void handleImport()}>
              开始导入
            </button>
          </div>
        ) : null}
        {importMessage ? <p className={styles.helper}>{importMessage}</p> : null}
        {errorMessage ? <p className={styles.error}>{errorMessage}</p> : null}
      </div>

      {formOpen ? (
        <div className={styles.panel}>
          <div className={styles.form}>
            <h2>{editingRecord ? '编辑考生' : '新增考生'}</h2>
            <div className={styles.formGrid}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-no">
                  考生编号
                </label>
                <input
                  id="examinee-no"
                  className={styles.input}
                  value={form.examineeNo}
                  disabled={Boolean(editingRecord)}
                  onChange={(event) => setForm((current) => ({ ...current, examineeNo: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-name">
                  姓名
                </label>
                <input
                  id="examinee-name"
                  className={styles.input}
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-gender">
                  性别
                </label>
                <select
                  id="examinee-gender"
                  className={styles.select}
                  value={form.gender}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, gender: event.target.value as ExamineeFormPayload['gender'] }))
                  }
                >
                  <option value="MALE">男</option>
                  <option value="FEMALE">女</option>
                </select>
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-id-card">
                  身份证号
                </label>
                <input
                  id="examinee-id-card"
                  className={styles.input}
                  value={form.idCardNo}
                  onChange={(event) => setForm((current) => ({ ...current, idCardNo: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-phone">
                  手机号
                </label>
                <input
                  id="examinee-phone"
                  className={styles.input}
                  value={form.phone}
                  onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-email">
                  邮箱
                </label>
                <input
                  id="examinee-email"
                  className={styles.input}
                  value={form.email}
                  onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="examinee-form-status">
                  状态
                </label>
                <select
                  id="examinee-form-status"
                  className={styles.select}
                  value={form.status}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, status: event.target.value as ExamineeFormPayload['status'] }))
                  }
                >
                  <option value="ENABLED">启用</option>
                  <option value="DISABLED">禁用</option>
                </select>
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="examinee-remark">
                备注
              </label>
              <textarea
                id="examinee-remark"
                className={styles.textarea}
                value={form.remark}
                onChange={(event) => setForm((current) => ({ ...current, remark: event.target.value }))}
              />
            </div>
            {formErrorMessage ? (
              <p role="alert" className={styles.error}>
                {formErrorMessage}
              </p>
            ) : null}
            <div className={styles.actions}>
              <button className={styles.primaryButton} type="button" onClick={() => void handleSave()}>
                保存考生
              </button>
              <button className={styles.secondaryButton} type="button" onClick={() => setFormOpen(false)}>
                取消
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <div className={styles.panel}>
        {loading ? <p className={styles.helper}>正在加载考生数据...</p> : null}
        {!loading && records.length === 0 ? <p className={styles.empty}>当前筛选条件下暂无考生数据。</p> : null}
        {!loading && records.length > 0 ? (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>考生编号</th>
                <th>姓名</th>
                <th>手机号</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{record.examineeNo}</td>
                  <td>{record.name}</td>
                  <td>{record.phone}</td>
                  <td>
                    <span className={record.status === 'ENABLED' ? styles.statusEnabled : styles.statusDisabled}>
                      {record.status === 'ENABLED' ? '启用' : '禁用'}
                    </span>
                  </td>
                  <td>
                    <div className={styles.rowActions}>
                      {canUpdate ? (
                        <button className={styles.secondaryButton} type="button" onClick={() => openEditForm(record)}>
                          编辑
                        </button>
                      ) : null}
                      {canToggleStatus ? (
                        <button className={styles.secondaryButton} type="button" onClick={() => void handleToggleStatus(record)}>
                          {record.status === 'ENABLED' ? '禁用' : '启用'}
                        </button>
                      ) : null}
                      {canDelete ? (
                        <button className={styles.dangerButton} type="button" onClick={() => void handleDelete(record)}>
                          删除
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}
