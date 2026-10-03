"use client";
import { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ className, children, ...props }: CardProps) {
  return (
    <div
      className={cn(
        "bg-white border border-slate-200 rounded-lg shadow-sm",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardHeader({ className, children, ...props }: CardProps) {
  return (
    <div className={cn("p-6 border-b border-slate-200", className)} {...props}>
      {children}
    </div>
  );
}

export function CardTitle({ className, children, ...props }: CardProps) {
  return (
    <h2
      className={cn("text-xl font-semibold text-slate-900", className)}
      {...props}
    >
      {children}
    </h2>
  );
}

export function CardDescription({ className, children, ...props }: CardProps) {
  return (
    <p className={cn("text-sm text-slate-500 mt-1", className)} {...props}>
      {children}
    </p>
  );
}

export function CardContent({ className, children, ...props }: CardProps) {
  return (
    <div className={cn("p-6", className)} {...props}>
      {children}
    </div>
  );
}

export function CardFooter({ className, children, ...props }: CardProps) {
  return (
    <div
      className={cn(
        "p-6 border-t border-slate-200 bg-slate-50 rounded-b-lg",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}
