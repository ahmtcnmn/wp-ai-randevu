"use client";
import { useEffect, useState, useRef, use, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import {
  conversationApi, ConversationResponse, MessageResponse,
} from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatRelative, formatTime } from "@/lib/utils/date";
import { cn } from "@/lib/utils/cn";
import { useSector } from "@/store/SectorContext";

export default function WhatsappDetayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const convId = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [conv, setConv] = useState<ConversationResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  async function load() {
    try {
      const c = await conversationApi.get(convId);
      setConv(c);
      const msgs = await conversationApi.messages(convId);
      setMessages(msgs);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    const interval = setInterval(load, 5000); // 5sn polling
    return () => clearInterval(interval);
  }, [convId]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  async function send(e: FormEvent) {
    e.preventDefault();
    if (!input.trim() || sending) return;
    setSending(true);
    try {
      await conversationApi.sendMessage(convId, { icerik: input.trim(), tur: "OUTBOUND" });
      setInput("");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    } finally {
      setSending(false);
    }
  }

  async function takeover() {
    try {
      await conversationApi.takeover(convId);
      toast.success("Konuşma devralındı");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }
  async function release() {
    try {
      await conversationApi.release(convId);
      toast.success("AI'a bırakıldı");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }
  async function close() {
    if (!confirm("Konuşmayı kapatmak istediğinize emin misiniz?")) return;
    try {
      await conversationApi.close(convId);
      toast.success("Kapatıldı");
      router.push("/whatsapp");
    } catch (err) { toast.error(extractApiError(err)); }
  }

  if (loading) return <div className="flex justify-center py-12"><Spinner size="lg" /></div>;
  if (error) return <div className="p-8"><Alert variant="error">{error}</Alert></div>;
  if (!conv) return null;

  const isClosed = conv.durum === "CLOSED";

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      {/* Top bar */}
      <div className="bg-white border-b border-slate-200 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3 min-w-0">
          <Button variant="ghost" size="sm" onClick={() => router.back()}>←</Button>
          <Avatar name={conv.customerName || conv.customerPhone} size="md" />
          <div className="min-w-0">
            <div className="font-semibold text-slate-900 truncate">
              {conv.customerName || conv.customerPhone}
            </div>
            <div className="text-xs text-slate-500">{conv.customerPhone}</div>
          </div>
          <Badge variant={conv.durum === "HUMAN_ACTIVE" ? "success" : conv.durum === "ACTIVE" ? "info" : "default"}>
            {conv.durum}
          </Badge>
        </div>
        <div className="flex items-center gap-2">
          {!isClosed && conv.durum !== "HUMAN_ACTIVE" && (
            <Button size="sm" onClick={takeover}>Devral</Button>
          )}
          {!isClosed && conv.durum === "HUMAN_ACTIVE" && (
            <Button size="sm" variant="secondary" onClick={release}>AI'a bırak</Button>
          )}
          {!isClosed && (
            <Button size="sm" variant="danger" onClick={close}>Kapat</Button>
          )}
        </div>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto bg-slate-50 p-4 space-y-3">
        {messages.length === 0 ? (
          <div className="text-center text-sm text-slate-500 mt-12">Henüz mesaj yok</div>
        ) : (
          messages.map((m) => {
            const isCustomer = m.senderType === "CUSTOMER";
            const isSystem = m.senderType === "SYSTEM";
            return (
              <div
                key={m.id}
                className={cn(
                  "max-w-[70%] rounded-lg p-3",
                  isSystem && "mx-auto bg-amber-100 text-amber-900 text-xs italic max-w-[80%] text-center",
                  isCustomer && "bg-white border border-slate-200 mr-auto",
                  !isCustomer && !isSystem && "bg-[var(--color-primary)] text-white ml-auto"
                )}
              >
                {!isSystem && (
                  <div className="text-xs opacity-70 mb-0.5">
                    {isCustomer ? (conv.customerName || labels.customerSingular) : (m.senderName || m.senderType)}
                    {" · "}
                    {formatTime(m.olusturmaTarihi)}
                  </div>
                )}
                <div className="text-sm whitespace-pre-wrap">{m.icerik}</div>
              </div>
            );
          })
        )}
      </div>

      {/* Input */}
      {!isClosed && (
        <form onSubmit={send} className="border-t border-slate-200 p-3 bg-white flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Mesajınızı yazın..."
            disabled={sending}
            className="flex-1 px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent disabled:bg-slate-50"
          />
          <Button type="submit" loading={sending} disabled={!input.trim()}>Gönder</Button>
        </form>
      )}
      {isClosed && (
        <div className="border-t border-slate-200 p-4 bg-slate-100 text-center text-sm text-slate-500">
          Bu konuşma kapatıldı. {labels.customerSingular} yeni mesaj atarsa otomatik açılır.
        </div>
      )}
    </div>
  );
}
