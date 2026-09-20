import { createFileRoute } from "@tanstack/react-router";
import { LauncherShell } from "@/components/launcher/launcher-shell";

export const Route = createFileRoute("/")({ component: Home });

function Home() {
  return <LauncherShell />;
}
