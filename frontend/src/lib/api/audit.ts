import api, { unwrap } from "./axios";
import { Page } from "@/types/api";

export interface AuditLogResponse {
  id: number;
  tenantId: number | null;
  userId: number | null;
  userEmail: string | null;
  userRol: string | null;
  action: string;
  entityType: string | null;
  entityId: number | null;
  details: Record<string, unknown> | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}

const BASE = "/api/v1/audit-logs";

export const auditApi = {
  list: async (page = 0, size = 50, action?: string): Promise<Page<AuditLogResponse>> => {
    const params: Record<string, string | number> = { page, size };
    if (action) params.action = action;
    const res = await api.get(BASE, { params });
    return unwrap<Page<AuditLogResponse>>(res.data);
  },
  forEntity: async (entityType: string, entityId: number): Promise<AuditLogResponse[]> => {
    const res = await api.get(`${BASE}/entity/${entityType}/${entityId}`);
    return unwrap<AuditLogResponse[]>(res.data);
  },
};
