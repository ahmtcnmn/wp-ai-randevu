import api, { unwrap } from "./axios";

export interface BackgroundJobResponse {
  id: number;
  jobType: string;
  payload: Record<string, unknown>;
  status: "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "DEAD";
  attemptCount: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface DeadLetterJobResponse {
  id: number;
  originalJobId: number;
  jobType: string;
  payload: Record<string, unknown>;
  errorMessage: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

const BASE = "/api/v1/jobs";

export const jobApi = {
  list: async (): Promise<BackgroundJobResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<BackgroundJobResponse[]>(res.data);
  },
  deadLetter: async (): Promise<DeadLetterJobResponse[]> => {
    const res = await api.get(`${BASE}/dead-letter`);
    return unwrap<DeadLetterJobResponse[]>(res.data);
  },
  retryDead: async (id: number): Promise<void> => {
    await api.post(`${BASE}/dead-letter/${id}/retry`);
  },
  resolveDead: async (id: number): Promise<void> => {
    await api.post(`${BASE}/dead-letter/${id}/resolve`);
  },
};
