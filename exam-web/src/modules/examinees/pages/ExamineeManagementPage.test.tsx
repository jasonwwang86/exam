import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockListExaminees,
  mockCreateExaminee,
  mockUpdateExaminee,
  mockDeleteExaminee,
  mockUpdateExamineeStatus,
  mockImportExaminees,
  mockExportExaminees,
} = vi.hoisted(() => ({
  mockListExaminees: vi.fn(),
  mockCreateExaminee: vi.fn(),
  mockUpdateExaminee: vi.fn(),
  mockDeleteExaminee: vi.fn(),
  mockUpdateExamineeStatus: vi.fn(),
  mockImportExaminees: vi.fn(),
  mockExportExaminees: vi.fn(),
}));

vi.mock('../services/examineeApi', () => ({
  listExaminees: mockListExaminees,
  createExaminee: mockCreateExaminee,
  updateExaminee: mockUpdateExaminee,
  deleteExaminee: mockDeleteExaminee,
  updateExamineeStatus: mockUpdateExamineeStatus,
  importExaminees: mockImportExaminees,
  exportExaminees: mockExportExaminees,
}));

describe('ExamineeManagementPage', () => {
  beforeEach(() => {
    mockListExaminees.mockReset();
    mockCreateExaminee.mockReset();
    mockUpdateExaminee.mockReset();
    mockDeleteExaminee.mockReset();
    mockUpdateExamineeStatus.mockReset();
    mockImportExaminees.mockReset();
    mockExportExaminees.mockReset();
    mockListExaminees.mockResolvedValue({
      total: 1,
      page: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          examineeNo: 'EX2026001',
          name: '张三',
          gender: 'MALE',
          idCardNo: '110101199001010011',
          phone: '13800000001',
          email: 'zhangsan@example.com',
          status: 'ENABLED',
          remark: '首批考生',
          updatedAt: '2026-04-21T10:00:00',
        },
      ],
    });
    mockCreateExaminee.mockResolvedValue({});
    mockUpdateExaminee.mockResolvedValue({});
    mockDeleteExaminee.mockResolvedValue({});
    mockUpdateExamineeStatus.mockResolvedValue({});
    mockImportExaminees.mockResolvedValue({ successCount: 1, failureCount: 0, failures: [] });
    mockExportExaminees.mockResolvedValue(new Blob(['test']));
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:mock'),
      revokeObjectURL: vi.fn(),
    });
    vi.stubGlobal('confirm', vi.fn(() => true));
  });

  it('loads examinees and supports querying by filters', async () => {
    const { ExamineeManagementPage } = await import('./ExamineeManagementPage');
    const user = userEvent.setup();

    render(<ExamineeManagementPage token="token-123" permissions={['examinee:read']} />);

    expect(await screen.findByRole('heading', { name: '考生管理' })).toBeInTheDocument();
    expect(screen.getByText('考生列表')).toBeInTheDocument();
    expect(screen.getByText('张三')).toBeInTheDocument();

    await user.type(screen.getByLabelText('关键字'), '张');
    await user.selectOptions(screen.getByLabelText('状态'), 'ENABLED');
    await user.click(screen.getByRole('button', { name: /查\s*询/ }));

    await waitFor(() => {
      expect(mockListExaminees).toHaveBeenLastCalledWith('token-123', {
        keyword: '张',
        status: 'ENABLED',
        page: 1,
        pageSize: 10,
      });
    });
  });

  it('validates required form fields before creating examinee', async () => {
    const { ExamineeManagementPage } = await import('./ExamineeManagementPage');
    const user = userEvent.setup();

    render(<ExamineeManagementPage token="token-123" permissions={['examinee:read', 'examinee:create']} />);

    await screen.findByRole('heading', { name: '考生管理' });
    await user.click(screen.getByRole('button', { name: /新\s*增/ }));
    await user.click(await screen.findByText('新增考生'));
    await user.click(screen.getByRole('button', { name: /创\s*建/ }));

    expect(screen.getByRole('alert')).toHaveTextContent('请填写考生编号、姓名、性别、身份证号、手机号和状态');
    expect(mockCreateExaminee).not.toHaveBeenCalled();
  });

  it('opens edit modal and updates examinee', async () => {
    const { ExamineeManagementPage } = await import('./ExamineeManagementPage');
    const user = userEvent.setup();

    render(<ExamineeManagementPage token="token-123" permissions={['examinee:read', 'examinee:update']} />);

    await screen.findByText('张三');
    await user.click(screen.getByRole('button', { name: /编\s*辑/ }));

    expect(await screen.findByRole('dialog', { name: '编辑考生' })).toBeInTheDocument();
    await user.clear(screen.getByLabelText('姓名'));
    await user.type(screen.getByLabelText('姓名'), '李四');
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(mockUpdateExaminee).toHaveBeenCalledWith('token-123', 1, {
        name: '李四',
        gender: 'MALE',
        idCardNo: '110101199001010011',
        phone: '13800000001',
        email: 'zhangsan@example.com',
        status: 'ENABLED',
        remark: '首批考生',
      });
    });
  });

  it('updates examinee status and refreshes the list', async () => {
    const { ExamineeManagementPage } = await import('./ExamineeManagementPage');
    const user = userEvent.setup();

    render(<ExamineeManagementPage token="token-123" permissions={['examinee:read', 'examinee:status']} />);

    await screen.findByText('张三');
    await user.click(screen.getByRole('button', { name: /禁\s*用/ }));

    await waitFor(() => {
      expect(mockUpdateExamineeStatus).toHaveBeenCalledWith('token-123', 1, 'DISABLED');
    });
    expect(mockListExaminees).toHaveBeenCalledTimes(2);
  });

  it('supports importing and exporting examinees', async () => {
    const { ExamineeManagementPage } = await import('./ExamineeManagementPage');
    const user = userEvent.setup();

    render(
      <ExamineeManagementPage
        token="token-123"
        permissions={['examinee:read', 'examinee:import', 'examinee:export']}
      />,
    );

    await screen.findByText('张三');
    await user.click(screen.getByRole('button', { name: /新\s*增/ }));
    await user.click(await screen.findByText('导入文件'));
    const file = new File(['demo'], 'examinees.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    fireEvent.change(screen.getByLabelText('导入文件'), {
      target: {
        files: [file],
      },
    });
    await user.click(screen.getByRole('button', { name: '开始导入' }));
    await user.click(screen.getByRole('button', { name: '导出当前结果' }));

    await waitFor(() => {
      expect(mockImportExaminees).toHaveBeenCalledWith('token-123', file);
    });
    expect(mockExportExaminees).toHaveBeenCalledWith('token-123', {
      keyword: '',
      status: '',
    });
  });
});
