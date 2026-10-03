"use client";
import { useState, useRef, useEffect, FormEvent } from "react";
import { chatApi } from "@/lib/api";
import { cn } from "@/lib/utils/cn";
import { extractApiError } from "@/hooks/useApiError";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  ts: number;
}

let counter = 0;

/**
 * 23-AI-CHAT-DETAY — Floating AI chat widget.
 * Sağ alt köşe, dashboard'da her sayfada.
 */
export function AiChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, loading]);

  async function send(e: FormEvent) {
    e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput("");
    const userMsg: Message = { id: `u-${++counter}`, role: "user", content: text, ts: Date.now() };
    setMessages((m) => [...m, userMsg]);
    setLoading(true);
    try {
      const yanit = await chatApi.send(text);
      setMessages((m) => [
        ...m,
        { id: `a-${++counter}`, role: "assistant", content: yanit, ts: Date.now() },
      ]);
    } catch (err) {
      setMessages((m) => [
        ...m,
        {
          id: `a-${++counter}`,
          role: "assistant",
          content: "Üzgünüm, yanıt oluşturamadım: " + extractApiError(err),
          ts: Date.now(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  async function clear() {
    setMessages([]);
    try {
      await chatApi.clearHistory();
    } catch {
      // ignore
    }
  }

  return (
    <>
      {/* Floating trigger button */}
      <button
        type="button"
        onClick={() => setOpen((s) => !s)}
        className={cn(
          "fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full shadow-lg flex items-center justify-center",
          "bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] text-white text-2xl",
          "transition-transform hover:scale-110 focus-visible:ring-4 focus-visible:ring-[var(--color-primary)]/30"
        )}
        aria-label={open ? "AI Asistanı Kapat" : "AI Asistanı Aç"}
      >
        {open ? "✕" : "🤖"}
      </button>

      {/* Panel */}
      {open && (
        <div className="fixed bottom-24 right-6 z-40 w-[min(360px,calc(100vw-3rem))] h-[520px] bg-white rounded-xl shadow-2xl flex flex-col border border-slate-200 overflow-hidden">
          {/* Header */}
          <div className="px-4 py-3 border-b border-slate-200 flex items-center justify-between bg-gradient-to-r from-[var(--color-primary)] to-[var(--color-accent)] text-white">
            <div className="flex items-center gap-2">
              <span className="text-xl">🤖</span>
              <div>
                <div className="text-sm font-semibold">AppointFlow Asistan</div>
                <div className="text-xs opacity-80 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 bg-green-300 rounded-full inline-block" />
                  Aktif
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={clear}
              className="text-xs opacity-80 hover:opacity-100"
              title="Sohbeti temizle"
            >
              🗑
            </button>
          </div>

          {/* Messages */}
          <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50">
            {messages.length === 0 && (
              <div className="text-center text-sm text-slate-500 mt-12">
                <div className="text-4xl mb-2">👋</div>
                <p>Merhaba! Size nasıl yardımcı olabilirim?</p>
                <p className="text-xs mt-2">Randevu, müşteri, hizmet vb. sorularınızı sorabilirsiniz.</p>
              </div>
            )}
            {messages.map((m) => (
              <div
                key={m.id}
                className={cn(
                  "max-w-[85%] rounded-lg px-3 py-2 text-sm",
                  m.role === "user"
                    ? "bg-[var(--color-primary)] text-white ml-auto"
                    : "bg-white text-slate-900 border border-slate-200"
                )}
              >
                {m.content}
              </div>
            ))}
            {loading && (
              <div className="bg-white text-slate-500 rounded-lg px-3 py-2 text-sm border border-slate-200 max-w-[85%]">
                <span className="inline-flex gap-1">
                  <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" />
                  <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "0.1s" }} />
                  <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "0.2s" }} />
                </span>
              </div>
            )}
          </div>

          {/* Input */}
          <form onSubmit={send} className="border-t border-slate-200 p-3 bg-white flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Mesajınızı yazın..."
              disabled={loading}
              className="flex-1 px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent disabled:bg-slate-50"
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="px-4 py-2 bg-[var(--color-primary)] text-white rounded-md text-sm font-medium hover:bg-[var(--color-primary-hover)] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Gönder
            </button>
          </form>
        </div>
      )}
    </>
  );
}
