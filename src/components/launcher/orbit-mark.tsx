import { cn } from "@/lib/utils";

export function OrbitMark({
  className,
  title = "OrbitX",
}: {
  className?: string;
  title?: string;
}) {
  return (
    <img
      src="/orbitx-icon.png"
      alt={title}
      className={cn("rounded-[22%] object-cover", className)}
    />
  );
}
