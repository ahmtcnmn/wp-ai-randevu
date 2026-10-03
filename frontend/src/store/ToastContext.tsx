"use client";
import {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from "react";
import { cn } from "@/lib/utils/cn";

export type ToastVariant = "success" | "error" | "warning" | "info";

export interface Toast {
  id: string;
  variant: ToastVariant;
  title?: string;
  message: string;
  duration?: number;
}

interface ToastContextType {
  toasts: Toast[];
  show: (toast: Omit<Toast, "id">) => void;
  success: (message: string, title?: string) => void;
  error: (message: string, title?: string) => void;
  warning: (message: string, title?: string) => void;
  info: (message: string, title?: string) => void;
  dismiss: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);

let counter = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const show = useCallback(
    (toast: Omit<Toast, "id">) => {
      const id = `toast-${++counter}`;
      const newToast: Toast = { id, duration: 4000, ...toast };
      setToasts((prev) => [...prev, newToast]);
      if (newToast.duration && newToast.duration > 0) {
        setTimeout(() => dismiss(id), newToast.duration);
      }
    },
    [dismiss]
  );

  const success = useCallback(
    (message: string, title?: string) => show({ variant: "success", message, title }),
    [show]
  );
  const error = useCallback(
    (message: string, title?: string) => show({ variant: "error", message, title }),
    [show]
  );
  const warning = useCallback(
    (message: string, title?: string) => show({ variant: "warning", message, title }),
    [show]
  );
  const info = useCallback(
    (message: string, title?: string) => show({ variant: "info", message, title }),
    [show]
  );

  return (
    <ToastContext.Provider value={{ toasts, show, success, error, warning, info, dismiss }}>
      {children}
      <ToastViewport toasts={toasts} dismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}

function ToastViewport({
  toasts,
  dismiss,
}: {
  toasts: Toast[];
  dismiss: (id: string) => void;
}) {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none w-80 max-w-[calc(100vw-2rem)]">
      {toasts.map((t) => (
        <ToastCard key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
      ))}
    </div>
  );
}

function ToastCard({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  const variantStyles: Record<ToastVariant, { bg: string; border: string; icon: string }> = {
    success: {
      bg: "bg-white",
      border: "border-l-4 border-l-green-500",
      icon: "✓",
    },
    error: {
      bg: "bg-white",
      border: "border-l-4 border-l-red-500",
      icon: "✕",
    },
    warning: {
      bg: "bg-white",
      border: "border-l-4 border-l-amber-500",
      icon: "⚠",
    },
    info: {
      bg: "bg-white",
      border: "border-l-4 border-l-blue-500",
      icon: "ℹ",
    },
  };

  const { bg, border, icon } = variantStyles[toast.variant];

  return (
    <div
      className={cn(
        "pointer-events-auto rounded-md shadow-lg p-4 flex gap-3 items-start animate-in slide-in-from-right",
        bg,
        border
      )}
      role="alert"
    >
      <span className="text-xl leading-none flex-shrink-0" aria-hidden>
        {icon}
      </span>
      <div className="flex-1 min-w-0">
        {toast.title && (
          <p className="font-semibold text-sm text-slate-900 mb-0.5">{toast.title}</p>
        )}
        <p className="text-sm text-slate-700">{toast.message}</p>
      </div>
      <button
        type="button"
        onClick={onDismiss}
        className="text-slate-400 hover:text-slate-700 text-lg leading-none flex-shrink-0"
        aria-label="Kapat"
      >
        ×
      </button>
    </div>
  );
}
