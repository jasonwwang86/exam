import axios from 'axios';
import { withTraceNo } from '../../../shared/utils/trace';
import type { DashboardSummary } from '../types';

const client = axios.create();

export async function fetchDashboardSummary(token: string) {
  const response = await client.get<DashboardSummary>(
    '/api/admin/dashboard/summary',
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}
