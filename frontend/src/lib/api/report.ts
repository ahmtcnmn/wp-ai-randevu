import api, { unwrap } from "./axios";

export interface AppointmentReportResponse {
  from: string;
  to: string;
  total: number;
  byStatus: Record<string, number>;
  revenueByStaff: Record<string, number>;
}

export interface RevenueReportResponse {
  from: string;
  to: string;
  totalRevenue: number;
  byStaff: Record<string, number>;
  byService: Record<string, number>;
  avgRevenuePerAppointment: number;
}

export interface CustomerReportResponse {
  from: string;
  to: string;
  total: number;
  segmentDistribution: Record<string, number>;
  newCustomers: number;
  churnRiskCount: number;
  blacklistedCount: number;
}

export interface CampaignReportResponse {
  from: string;
  to: string;
  slotCampaignsTotal: number;
  slotCampaignsFilled: number;
  slotFillRate: number;
  segmentCampaignsTotal: number;
}

const BASE = "/api/v1/reports";

export const reportApi = {
  appointments: async (dateFrom: string, dateTo: string): Promise<AppointmentReportResponse> => {
    const res = await api.get(`${BASE}/appointments`, { params: { dateFrom, dateTo } });
    return unwrap<AppointmentReportResponse>(res.data);
  },
  revenue: async (dateFrom: string, dateTo: string): Promise<RevenueReportResponse> => {
    const res = await api.get(`${BASE}/revenue`, { params: { dateFrom, dateTo } });
    return unwrap<RevenueReportResponse>(res.data);
  },
  customers: async (dateFrom?: string, dateTo?: string): Promise<CustomerReportResponse> => {
    const params: Record<string, string> = {};
    if (dateFrom) params.dateFrom = dateFrom;
    if (dateTo) params.dateTo = dateTo;
    const res = await api.get(`${BASE}/customers`, { params });
    return unwrap<CustomerReportResponse>(res.data);
  },
  campaigns: async (dateFrom?: string, dateTo?: string): Promise<CampaignReportResponse> => {
    const params: Record<string, string> = {};
    if (dateFrom) params.dateFrom = dateFrom;
    if (dateTo) params.dateTo = dateTo;
    const res = await api.get(`${BASE}/campaigns`, { params });
    return unwrap<CampaignReportResponse>(res.data);
  },
};
