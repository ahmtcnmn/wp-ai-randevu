import api, { unwrap } from "./axios";

export type CommissionType = "PERCENTAGE" | "SALARY_PLUS_BONUS";
export type CommissionScope = "SERVICE" | "PRODUCT";

export interface CommissionRuleRequest {
  staffId?: number | null;
  commissionType: CommissionType;
  rate: number;
  bonusThreshold?: number;
  aktif?: boolean;
  scope?: CommissionScope;
  productId?: number | null;
  productKategori?: string | null;
}

export interface CommissionRuleResponse {
  id: number;
  tenantId: number;
  staffId: number | null;
  commissionType: CommissionType;
  rate: number;
  bonusThreshold: number | null;
  aktif: boolean;
  scope: CommissionScope;
  productId: number | null;
  productKategori: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EarningResponse {
  id: number;
  randevuId: number;
  staffId: number;
  commissionRuleId: number | null;
  earningPeriodId: number | null;
  commissionType: CommissionType;
  rateSnapshot: number;
  grossAmount: number;
  commissionAmount: number;
  netAmount: number;
  status: "PENDING" | "COLLECTED";
  productSaleId?: number | null;
  createdAt: string;
}

export interface EarningsSummaryResponse {
  staffId: number;
  totalGross: number;
  totalCommission: number;
  pendingCommission: number;
  collectedCommission: number;
  totalEarnings: number;
  pendingEarnings: number;
  collectedEarnings: number;
}

export interface EarningPeriodRequest {
  staffId: number;
  periodStart: string;
  periodEnd: string;
}

export interface EarningPeriodResponse {
  id: number;
  tenantId: number;
  staffId: number;
  periodStart: string;
  periodEnd: string;
  totalGross: number;
  totalCommission: number;
  totalNet: number;
  status: "PENDING" | "COLLECTED";
  collectedAt: string | null;
  createdAt: string;
}

const BASE = "/api/v1/commission";

export const commissionApi = {
  // Rules
  listRules: async (scope?: CommissionScope): Promise<CommissionRuleResponse[]> => {
    const res = await api.get(`${BASE}/rules`, { params: scope ? { scope } : undefined });
    return unwrap<CommissionRuleResponse[]>(res.data);
  },
  createRule: async (body: CommissionRuleRequest): Promise<CommissionRuleResponse> => {
    const res = await api.post(`${BASE}/rules`, body);
    return unwrap<CommissionRuleResponse>(res.data);
  },
  updateRule: async (id: number, body: CommissionRuleRequest): Promise<CommissionRuleResponse> => {
    const res = await api.put(`${BASE}/rules/${id}`, body);
    return unwrap<CommissionRuleResponse>(res.data);
  },
  deleteRule: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/rules/${id}`);
  },

  // Periods
  listPeriods: async (): Promise<EarningPeriodResponse[]> => {
    const res = await api.get(`${BASE}/periods`);
    return unwrap<EarningPeriodResponse[]>(res.data);
  },
  createPeriod: async (body: EarningPeriodRequest): Promise<EarningPeriodResponse> => {
    const res = await api.post(`${BASE}/periods`, body);
    return unwrap<EarningPeriodResponse>(res.data);
  },
  collectPeriod: async (id: number): Promise<EarningPeriodResponse> => {
    const res = await api.post(`${BASE}/periods/${id}/collect`);
    return unwrap<EarningPeriodResponse>(res.data);
  },
  periodEarnings: async (id: number): Promise<EarningResponse[]> => {
    const res = await api.get(`${BASE}/periods/${id}/earnings`);
    return unwrap<EarningResponse[]>(res.data);
  },

  // Earnings
  myEarnings: async (): Promise<EarningResponse[]> => {
    const res = await api.get(`${BASE}/my-earnings`);
    return unwrap<EarningResponse[]>(res.data);
  },
  mySummary: async (): Promise<EarningsSummaryResponse> => {
    const res = await api.get(`${BASE}/my-summary`);
    return unwrap<EarningsSummaryResponse>(res.data);
  },
  staffEarnings: async (staffId: number): Promise<EarningResponse[]> => {
    const res = await api.get(`${BASE}/staff/${staffId}/earnings`);
    return unwrap<EarningResponse[]>(res.data);
  },
  staffSummary: async (staffId: number): Promise<EarningsSummaryResponse> => {
    const res = await api.get(`${BASE}/staff/${staffId}/summary`);
    return unwrap<EarningsSummaryResponse>(res.data);
  },
};
