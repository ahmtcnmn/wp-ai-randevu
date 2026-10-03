import api from "./axios";

export interface ContactFormRequest {
  ad: string;
  soyad: string;
  telefon: string;
  email: string;
  mesaj: string;
}

const BASE = "/api/v1/public/contact";

export const publicContactApi = {
  submit: async (body: ContactFormRequest): Promise<void> => {
    await api.post(BASE, body);
  },
};
