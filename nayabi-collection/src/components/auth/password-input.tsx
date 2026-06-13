"use client";

import * as React from "react";
import { Eye, EyeOff } from "lucide-react";
import { GlassInput } from "@/components/ui";
import { cn } from "@/lib/utils";

interface PasswordInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  showStrength?: boolean;
}

/* Simple heuristic: 0–4 */
function scorePassword(pw: string): number {
  if (!pw) return 0;
  let score = 0;
  if (pw.length >= 8) score++;
  if (pw.length >= 12) score++;
  if (/[a-zA-Z]/.test(pw) && /[0-9]/.test(pw)) score++;
  if (/[^a-zA-Z0-9]/.test(pw)) score++;
  return score;
}

const strengthMeta = [
  { label: "", color: "" },
  { label: "Weak", color: "#FCA5A5" },
  { label: "Fair", color: "#FCD34D" },
  { label: "Strong", color: "#86EFAC" },
  { label: "Very strong", color: "#86EFAC" },
] as const;

export function PasswordInput({ label, error, showStrength, ...props }: PasswordInputProps) {
  const [visible, setVisible] = React.useState(false);
  const [value, setValue] = React.useState("");
  const score = scorePassword(value);
  const meta = strengthMeta[score];

  return (
    <div className="flex flex-col gap-1.5">
      <GlassInput
        label={label}
        type={visible ? "text" : "password"}
        error={error}
        rightIcon={
          visible ? (
            <EyeOff className="h-4 w-4" aria-hidden="true" />
          ) : (
            <Eye className="h-4 w-4" aria-hidden="true" />
          )
        }
        onRightIconClick={() => setVisible((v) => !v)}
        onChange={(e) => {
          setValue(e.target.value);
          props.onChange?.(e);
        }}
        {...props}
      />
      {showStrength && value && (
        <div aria-live="polite">
          <div className="flex gap-1" aria-hidden="true">
            {[1, 2, 3, 4].map((i) => (
              <span
                key={i}
                className={cn("h-1 flex-1 rounded-full transition-colors")}
                style={{
                  backgroundColor: i <= score ? meta.color : "rgba(255,255,255,0.1)",
                }}
              />
            ))}
          </div>
          <p className="mt-1 text-xs" style={{ color: meta.color }}>
            {meta.label}
          </p>
        </div>
      )}
    </div>
  );
}
