"use client";
import { cn } from "@/lib/utils/cn";

interface AvatarProps {
  name: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeClasses = {
  sm: "w-8 h-8 text-xs",
  md: "w-10 h-10 text-sm",
  lg: "w-14 h-14 text-base",
};

const colors = [
  "bg-blue-500", "bg-emerald-500", "bg-amber-500",
  "bg-rose-500", "bg-purple-500", "bg-cyan-500", "bg-pink-500",
];

function hash(str: string) {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = ((h << 5) - h) + str.charCodeAt(i);
  return Math.abs(h);
}

export function Avatar({ name, size = "md", className }: AvatarProps) {
  const initials = name
    .split(" ")
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
  const color = colors[hash(name) % colors.length];

  return (
    <div
      className={cn(
        "inline-flex items-center justify-center rounded-full text-white font-semibold flex-shrink-0",
        sizeClasses[size],
        color,
        className
      )}
    >
      {initials || "?"}
    </div>
  );
}
