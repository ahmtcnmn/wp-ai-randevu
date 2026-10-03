"use client";
import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface Tab {
  id: string;
  label: ReactNode;
  badge?: ReactNode;
}

interface TabsProps {
  tabs: Tab[];
  active: string;
  onChange: (id: string) => void;
  className?: string;
}

export function Tabs({ tabs, active, onChange, className }: TabsProps) {
  return (
    <div
      className={cn(
        "flex gap-1 border-b border-slate-200 overflow-x-auto",
        className
      )}
      role="tablist"
    >
      {tabs.map((t) => {
        const isActive = t.id === active;
        return (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(t.id)}
            className={cn(
              "px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 -mb-px transition-colors",
              isActive
                ? "text-[var(--color-primary)] border-[var(--color-primary)]"
                : "text-slate-500 border-transparent hover:text-slate-900 hover:border-slate-300"
            )}
          >
            {t.label}
            {t.badge !== undefined && (
              <span className="ml-2">{t.badge}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}
