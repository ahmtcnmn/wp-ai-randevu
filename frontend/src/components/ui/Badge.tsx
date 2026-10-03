"use client";
import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type Variant = "default" | "primary" | "success" | "warning" | "danger" | "info";

interface BadgeProps {
  variant?: Variant;
  children: ReactNode;
  className?: string;
}

const variantStyles: Record<Variant, string> = {
  default: "bg-slate-100 text-slate-700",
  primary: "bg-[var(--color-primary-soft)] text-[var(--color-primary)]",
  success: "bg-green-100 text-green-700",
  warning: "bg-amber-100 text-amber-700",
  danger: "bg-red-100 text-red-700",
  info: "bg-blue-100 text-blue-700",
};

export function Badge({ variant = "default", children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium",
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
