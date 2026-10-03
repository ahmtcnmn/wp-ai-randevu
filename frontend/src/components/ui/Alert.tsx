"use client";
import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type Variant = "info" | "success" | "warning" | "error";

interface AlertProps {
  variant?: Variant;
  title?: string;
  children?: ReactNode;
  className?: string;
}

const variantStyles: Record<Variant, { bg: string; border: string; text: string; icon: string }> = {
  info: {
    bg: "bg-blue-50",
    border: "border-blue-200",
    text: "text-blue-900",
    icon: "ℹ",
  },
  success: {
    bg: "bg-green-50",
    border: "border-green-200",
    text: "text-green-900",
    icon: "✓",
  },
  warning: {
    bg: "bg-amber-50",
    border: "border-amber-200",
    text: "text-amber-900",
    icon: "⚠",
  },
  error: {
    bg: "bg-red-50",
    border: "border-red-200",
    text: "text-red-900",
    icon: "✕",
  },
};

export function Alert({ variant = "info", title, children, className }: AlertProps) {
  const { bg, border, text, icon } = variantStyles[variant];
  return (
    <div
      className={cn(
        "border rounded-md p-4 flex gap-3 items-start",
        bg,
        border,
        text,
        className
      )}
      role="alert"
    >
      <span className="text-lg leading-none flex-shrink-0" aria-hidden>
        {icon}
      </span>
      <div className="flex-1 min-w-0 text-sm">
        {title && <p className="font-semibold mb-0.5">{title}</p>}
        {children}
      </div>
    </div>
  );
}
