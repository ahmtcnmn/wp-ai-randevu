"use client";
import { InputHTMLAttributes, forwardRef, ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helper?: string;
  leftAdornment?: ReactNode;
  rightAdornment?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helper, leftAdornment, rightAdornment, className, id, ...props }, ref) => {
    const inputId = id || props.name;
    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-slate-700 mb-1.5"
          >
            {label}
            {props.required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
        )}
        <div
          className={cn(
            "flex items-center w-full bg-white border rounded-md transition-colors",
            "focus-within:ring-2 focus-within:ring-[var(--color-primary)] focus-within:ring-offset-0 focus-within:border-transparent",
            error
              ? "border-red-400 focus-within:ring-red-500"
              : "border-slate-300 hover:border-slate-400"
          )}
        >
          {leftAdornment && (
            <span className="pl-3 text-slate-500 flex items-center">{leftAdornment}</span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              "flex-1 px-3 h-10 bg-transparent outline-none text-sm text-slate-900",
              "placeholder:text-slate-400",
              "disabled:bg-slate-50 disabled:text-slate-500 disabled:cursor-not-allowed",
              className
            )}
            {...props}
          />
          {rightAdornment && (
            <span className="pr-3 text-slate-500 flex items-center">{rightAdornment}</span>
          )}
        </div>
        {error && <p className="mt-1.5 text-sm text-red-600">{error}</p>}
        {!error && helper && (
          <p className="mt-1.5 text-sm text-slate-500">{helper}</p>
        )}
      </div>
    );
  }
);
Input.displayName = "Input";
