import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/** A voxel player face — used as the avatar everywhere. */
export function PlayerHead({
  className,
  framed = false,
}: {
  className?: string;
  framed?: boolean;
}) {
  return (
    <span
      className={cn(
        "relative block shrink-0 overflow-hidden rounded-xl bg-skin",
        framed &&
          "border border-white/10 shadow-[0_10px_30px_rgba(0,0,0,0.45),inset_0_1px_0_rgba(255,255,255,0.12)]",
        className,
      )}
      aria-hidden
    >
      <span className="absolute inset-x-0 top-0 h-[26%] bg-hair" />
      <span className="absolute top-[46%] left-[20%] size-[14%] bg-bg" />
      <span className="absolute top-[46%] right-[20%] size-[14%] bg-bg" />
      <span className="absolute bottom-[16%] left-1/2 h-[7%] w-[36%] -translate-x-1/2 bg-accent-dark/85" />
    </span>
  );
}

export function PlayerPreview({ name }: { name: string }) {
  return (
    <div className="relative mx-auto flex h-full min-h-56 w-full max-w-sm items-end justify-center">
      {/* ground glow */}
      <div className="pointer-events-none absolute inset-x-10 bottom-9 h-16 rounded-[100%] bg-black/60 blur-xl" />
      <div className="pointer-events-none absolute inset-x-16 bottom-10 h-8 rounded-[100%] bg-accent/25 blur-lg" />
      <div className="player-bob relative flex flex-col items-center">
        <div
          className="orbit-ring absolute top-[40%] h-44 w-44 rounded-full border-[13px] border-accent/80"
          style={{ transform: "rotateX(68deg)" }}
          aria-hidden
        />
        <VoxelPlayer />
        <p className="relative z-10 mt-3 font-display text-xl font-semibold tracking-wide text-fg drop-shadow-[0_2px_6px_rgba(0,0,0,0.8)]">
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
        <span className="absolute inset-x-0 top-0 h-3 bg-hair" />
        <span className="absolute top-[44%] left-[22%] h-2 w-2 bg-bg" />
        <span className="absolute top-[44%] right-[22%] h-2 w-2 bg-bg" />
        <span className="absolute bottom-[16%] left-1/2 h-1 w-4 -translate-x-1/2 bg-accent-dark/85" />
      </Box>
      <Box className="mt-0.5 h-16 w-12 bg-accent">
        <span className="absolute inset-x-2 top-3 h-8 bg-black/20" />
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
