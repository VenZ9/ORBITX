import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PlayerPreview({ name }: { name: string }) {
  return (
    <div className="relative mx-auto flex h-full min-h-64 w-full max-w-sm items-end justify-center">
      <div className="pointer-events-none absolute inset-x-8 bottom-8 h-16 rounded-[100%] bg-bg/70 blur-xl" />
      <div className="player-bob relative flex flex-col items-center">
        <div
          className="orbit-ring absolute top-[38%] h-44 w-44 rounded-full border-[14px] border-accent/90"
          style={{ transform: "rotateX(68deg)" }}
          aria-hidden
        />
        <VoxelPlayer />
        <p className="relative z-10 mt-3 font-display text-xl font-semibold tracking-wide text-fg drop-shadow">
          {name}
        </p>
      </div>
    </div>
  );
}

function VoxelPlayer() {
  return (
    <div className="relative z-10 flex flex-col items-center" aria-hidden>
      <Box className="h-11 w-11 bg-skin shadow-inner">
        <span className="absolute top-[42%] left-[22%] h-2 w-2 bg-bg" />
        <span className="absolute top-[42%] right-[22%] h-2 w-2 bg-bg" />
        <span className="absolute bottom-[18%] left-1/2 h-1 w-4 -translate-x-1/2 bg-accent-dark/80" />
        <span className="absolute inset-x-0 top-0 h-3 bg-hair" />
      </Box>
      <Box className="mt-0.5 h-16 w-12 bg-accent">
        <span className="absolute inset-x-2 top-3 h-8 bg-accent-dark/40" />
      </Box>
      <div className="flex gap-1">
        <Box className="h-14 w-5 bg-pants" />
        <Box className="h-14 w-5 bg-pants-2" />
      </div>
    </div>
  );
}

function Box({
  className,
  children,
}: {
  className?: string;
  children?: ReactNode;
}) {
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-[2px] shadow-[4px_6px_0_rgba(0,0,0,0.35)]",
        className,
      )}
    >
      {children}
    </div>
  );
}
