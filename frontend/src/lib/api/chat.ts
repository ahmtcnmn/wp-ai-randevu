import api, { unwrap } from "./axios";

/**
 * AI Chat — /api/chat (v1 prefix YOK)
 * Backend ApiResponse<Map<String,String>> ile sarıyor → unwrap ile data.yanit'a inilir.
 */
export const chatApi = {
  send: async (mesaj: string): Promise<string> => {
    const res = await api.post("/api/chat", { mesaj });
    const data = unwrap<{ yanit?: string }>(res.data);
    return data?.yanit ?? "";
  },
  clearHistory: async (): Promise<void> => {
    await api.delete("/api/chat/history");
  },
};
