import { useState, type ReactNode } from "react";
import {
  ArrowLeft,
  Blocks,
  Check,
  Download,
  Gamepad2,
  Home,
  Play,
  Plus,
  Settings,
  Share2,
  Trash2,
  User,
  Wrench,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  CATALOG,
  selectedAccount,
  selectedVersion,
  useLauncher,
  type Loader,
  type NavId,
} from "@/lib/launcher-store";
import { OrbitMark } from "./orbit-mark";
import { PlayerPreview } from "./player-preview";

const NAV: { id: NavId; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Home", icon: Home },
  { id: "manage", label: "Manage", icon: Wrench },
  { id: "download", label: "Download", icon: Download },
  { id: "controller", label: "Controller", icon: Gamepad2 },
  { id: "multiplayer", label: "Multiplayer", icon: Share2 },
  { id: "settings", label: "Settings", icon: Settings },
];

export function LauncherShell() {
  const nav = useLauncher((s) => s.nav);
  const launching = useLauncher((s) => s.launching);

  return (
    <div className="relative min-h-dvh overflow-hidden bg-bg text-fg">
      <div
        className="pointer-events-none absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: "url(/launcher-bg.jpg)" }}
      />
      <div className="pointer-events-none absolute inset-0 bg-linear-to-br from-bg/55 via-bg/25 to-accent-dark/45" />

      <div className="relative z-10 mx-auto flex min-h-dvh max-w-[1400px] flex-col md:flex-row">
        <NavRail />
        <main className="flex min-h-0 min-w-0 flex-1 flex-col px-3 py-3 md:px-4 md:py-4">
          <HeaderBar />
          <div className="mt-3 min-h-0 flex-1 overflow-y-auto">
            {nav === "home" && <HomeView />}
            {nav === "manage" && <ManageView />}
            {nav === "download" && <DownloadView />}
            {nav === "controller" && <ControllerView />}
            {nav === "multiplayer" && <MultiplayerView />}
            {nav === "settings" && <SettingsView />}
          </div>
        </main>
        <RightDock />
      </div>

      {launching && <LaunchOverlay />}
    </div>
  );
}

function NavRail() {
  const nav = useLauncher((s) => s.nav);
  const setNav = useLauncher((s) => s.setNav);
  return (
    <nav
      className="flex shrink-0 items-center gap-1 overflow-x-auto border-border/80 bg-bg/88 px-2 py-2 backdrop-blur-md md:w-[72px] md:flex-col md:overflow-visible md:border-r md:py-5"
      aria-label="Launcher"
    >
      <OrbitMark className="hidden size-11 md:mb-4 md:block" />
      {NAV.map((item) => {
        const Icon = item.icon;
        const active = nav === item.id;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => setNav(item.id)}
            title={item.label}
            className={cn(
              "flex size-11 shrink-0 items-center justify-center rounded-md transition-colors duration-150",
              active
                ? "bg-accent text-fg"
                : "text-muted hover:bg-surface-2 hover:text-fg",
            )}
            aria-current={active ? "page" : undefined}
          >
            <Icon className="size-5" strokeWidth={1.75} />
            <span className="sr-only">{item.label}</span>
          </button>
        );
      })}
      <button
        type="button"
        onClick={() => setNav("home")}
        className="ml-auto flex size-11 items-center justify-center rounded-md text-muted hover:bg-surface-2 hover:text-fg md:mt-auto md:ml-0"
        title="Back"
      >
        <ArrowLeft className="size-5" strokeWidth={1.75} />
        <span className="sr-only">Back</span>
      </button>
    </nav>
  );
}

function HeaderBar() {
  const name = useLauncher((s) => s.launcherName);
  const nav = useLauncher((s) => s.nav);
  const label = NAV.find((n) => n.id === nav)?.label ?? "Home";
  return (
    <div className="flex items-center gap-3">
      <OrbitMark className="size-9 md:hidden" />
      <div className="min-w-0">
        <p className="font-display text-[1.65rem] leading-none font-semibold tracking-wide text-balance">
          {name}{" "}
          <span className="text-muted">1.3.3.3</span>
        </p>
        <p className="mt-1 text-xs tracking-wide text-subtle uppercase">{label}</p>
      </div>
    </div>
  );
}

function Panel({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "rounded-xl border border-border/80 bg-surface/78 p-4 shadow-[0_12px_40px_rgba(0,0,0,0.28)] backdrop-blur-md",
        className,
      )}
    >
      {children}
    </div>
  );
}

function HomeView() {
  const account = useLauncher(selectedAccount);
  return (
    <div className="grid gap-4 lg:h-full lg:grid-cols-[minmax(0,0.42fr)_minmax(0,0.58fr)]">
      <Panel className="flex flex-col lg:h-full">
        <h2 className="text-center font-display text-lg font-semibold tracking-wide">
          Notice
        </h2>
        <div className="my-3 h-px bg-border" />
        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto text-pretty text-sm leading-relaxed text-muted">
          <p>
            OrbitX Launcher is a rebrand of Fold Craft Launcher — same Java
            Edition stack on Android, new name, crimson orbit mark, and a
            darker UI.
          </p>
          <p>
            Full versions, Forge / Fabric / Quilt / NeoForge, custom controls,
            shaders, and LAN via Terracotta are still here. Game files now live
            in <span className="font-mono text-fg">/OrbitX/.minecraft</span>.
          </p>
          <p>
            This preview is the launcher chrome. Build the APK from the
            OrbitX-Launcher repo to run the game on a device.
          </p>
        </div>
        <div className="my-3 h-px bg-border" />
        <p className="text-center text-xs text-subtle">2026.09.19</p>
      </Panel>
      <div className="hidden min-h-72 lg:block">
        <PlayerPreview name={account?.name ?? "Orbit"} />
      </div>
    </div>
  );
}

function ManageView() {
  const versions = useLauncher((s) => s.versions);
  const selectedId = useLauncher((s) => s.selectedVersionId);
  const selectVersion = useLauncher((s) => s.selectVersion);
  const removeVersion = useLauncher((s) => s.removeVersion);
  const setNav = useLauncher((s) => s.setNav);

  return (
    <div className="space-y-3">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h2 className="font-display text-2xl font-semibold tracking-wide">
            Versions
          </h2>
          <p className="text-sm text-muted">Installed profiles on this device</p>
        </div>
        <button
          type="button"
          onClick={() => setNav("download")}
          className="inline-flex h-11 items-center gap-2 rounded-md bg-accent px-4 text-sm font-medium text-fg"
        >
          <Plus className="size-4" />
          Install
        </button>
      </div>
      <ul className="space-y-2">
        {versions.map((v) => {
          const active = v.id === selectedId;
          return (
            <li key={v.id}>
              <div
                className={cn(
                  "flex items-center gap-3 rounded-lg border px-3 py-3 transition-colors",
                  active
                    ? "border-accent bg-accent/15"
                    : "border-border bg-surface/70 hover:border-accent/50",
                )}
              >
                <button
                  type="button"
                  onClick={() => selectVersion(v.id)}
                  className="flex min-w-0 flex-1 items-center gap-3 text-left"
                >
                  <span className="flex size-10 items-center justify-center rounded-sm bg-surface-2 text-accent">
                    <Blocks className="size-5" />
                  </span>
                  <span className="min-w-0">
                    <span className="block truncate font-medium">{v.name}</span>
                    <span className="block text-xs text-subtle">
                      {v.mc}
                      {v.loaderVer ? ` · ${v.loader} ${v.loaderVer}` : ` · ${v.loader}`}
                      {v.lastPlayed ? ` · ${v.lastPlayed}` : ""}
                    </span>
                  </span>
                </button>
                {active && <Check className="size-4 shrink-0 text-accent-soft" />}
                <button
                  type="button"
                  onClick={() => removeVersion(v.id)}
                  className="flex size-10 items-center justify-center rounded-md text-subtle hover:bg-surface-2 hover:text-fg"
                  title="Remove"
                >
                  <Trash2 className="size-4" />
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function DownloadView() {
  const [loader, setLoader] = useState<Loader>("Vanilla");
  const [busy, setBusy] = useState<string | null>(null);
  const installVersion = useLauncher((s) => s.installVersion);
  const versions = useLauncher((s) => s.versions);
  const loaders: Loader[] = ["Vanilla", "Fabric", "Forge", "Quilt", "NeoForge"];

  function install(mc: string, type: "release" | "snapshot") {
    const id =
      loader === "Vanilla" ? mc : `${mc}-${loader.toLowerCase()}`;
    setBusy(id);
    window.setTimeout(() => {
      installVersion({
        id,
        name: loader === "Vanilla" ? mc : `${mc}-${loader}`,
        mc,
        loader,
        loaderVer: loader === "Vanilla" ? undefined : "latest",
        installed: true,
        lastPlayed: type === "snapshot" ? "Snapshot" : "Just now",
      });
      setBusy(null);
    }, 900);
  }

  return (
    <div className="space-y-4">
      <div>
        <h2 className="font-display text-2xl font-semibold tracking-wide">
          Download
        </h2>
        <p className="text-sm text-muted">
          Install a Minecraft version. Loaders apply at install time.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        {loaders.map((l) => (
          <button
            key={l}
            type="button"
            onClick={() => setLoader(l)}
            className={cn(
              "h-10 rounded-md px-3 text-sm font-medium",
              loader === l
                ? "bg-accent text-fg"
                : "bg-surface-2 text-muted hover:text-fg",
            )}
          >
            {l}
          </button>
        ))}
      </div>
      <ul className="divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface/70">
        {CATALOG.map((item) => {
          const id =
            loader === "Vanilla"
              ? item.mc
              : `${item.mc}-${loader.toLowerCase()}`;
          const have = versions.some((v) => v.id === id);
          return (
            <li
              key={item.id + loader}
              className="flex items-center gap-3 px-3 py-3"
            >
              <div className="min-w-0 flex-1">
                <p className="font-medium">{item.mc}</p>
                <p className="text-xs text-subtle">
                  {item.type} · {item.date}
                </p>
              </div>
              <button
                type="button"
                disabled={have || busy === id}
                onClick={() => install(item.mc, item.type)}
                className={cn(
                  "h-10 min-w-24 rounded-md px-3 text-sm font-medium",
                  have
                    ? "bg-surface-2 text-subtle"
                    : "bg-accent text-fg disabled:opacity-60",
                )}
              >
                {have ? "Installed" : busy === id ? "Installing…" : "Install"}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function ControllerView() {
  const layouts = useLauncher((s) => s.layouts);
  const selected = useLauncher((s) => s.selectedLayoutId);
  const selectLayout = useLauncher((s) => s.selectLayout);
  return (
    <div className="space-y-4">
      <div>
        <h2 className="font-display text-2xl font-semibold tracking-wide">
          Control layouts
        </h2>
        <p className="text-sm text-muted">
          Touch mappings used in-game. Compatible with ZalithLauncher2 import.
        </p>
      </div>
      <ul className="grid gap-3 sm:grid-cols-2">
        {layouts.map((l) => {
          const active = l.id === selected;
          return (
            <li key={l.id}>
              <button
                type="button"
                onClick={() => selectLayout(l.id)}
                className={cn(
                  "flex h-full w-full flex-col items-start rounded-lg border p-4 text-left",
                  active
                    ? "border-accent bg-accent/15"
                    : "border-border bg-surface/70 hover:border-accent/50",
                )}
              >
                <span className="flex items-center gap-2 font-medium">
                  <Gamepad2 className="size-4 text-accent-soft" />
                  {l.name}
                </span>
                <span className="mt-2 text-sm text-muted">
                  {l.author} · {l.buttons} buttons
                </span>
                {active && (
                  <span className="mt-3 text-xs tracking-wide text-accent-soft uppercase">
                    Active
                  </span>
                )}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function MultiplayerView() {
  const [mode, setMode] = useState<"host" | "guest">("host");
  const [code] = useState("ORBIT-7K2Q");
  const [join, setJoin] = useState("");
  const [status, setStatus] = useState("");

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div>
        <h2 className="font-display text-2xl font-semibold tracking-wide">
          Multiplayer
        </h2>
        <p className="text-sm text-muted">
          Terracotta mesh — play LAN across the internet, including offline
          accounts.
        </p>
      </div>
      <div className="flex gap-2">
        {(["host", "guest"] as const).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => {
              setMode(m);
              setStatus("");
            }}
            className={cn(
              "h-11 flex-1 rounded-md text-sm font-medium capitalize",
              mode === m ? "bg-accent text-fg" : "bg-surface-2 text-muted",
            )}
          >
            {m}
          </button>
        ))}
      </div>
      <Panel>
        {mode === "host" ? (
          <div className="space-y-3">
            <p className="text-sm text-muted">
              Open to LAN in-game, then share this invite code.
            </p>
            <p className="font-mono text-2xl tracking-[0.2em] text-fg">{code}</p>
            <button
              type="button"
              onClick={() => {
                void navigator.clipboard?.writeText(code);
                setStatus("Invite code copied");
              }}
              className="h-11 w-full rounded-md bg-accent text-sm font-medium"
            >
              Copy invite code
            </button>
          </div>
        ) : (
          <form
            className="space-y-3"
            onSubmit={(e) => {
              e.preventDefault();
              setStatus(
                join.trim()
                  ? `Joining ${join.trim().toUpperCase()}… grant VPN permission on device.`
                  : "Enter a host invite code",
              );
            }}
          >
            <label className="block text-sm text-muted">
              Invite code
              <input
                value={join}
                onChange={(e) => setJoin(e.target.value)}
                className="mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 text-fg outline-none focus:border-accent"
                placeholder="ORBIT-XXXX"
              />
            </label>
            <button
              type="submit"
              className="h-11 w-full rounded-md bg-accent text-sm font-medium"
            >
              Join session
            </button>
          </form>
        )}
        {status && <p className="mt-3 text-sm text-accent-soft">{status}</p>}
      </Panel>
    </div>
  );
}

function SettingsView() {
  const ram = useLauncher((s) => s.ramMb);
  const setRam = useLauncher((s) => s.setRamMb);
  const renderer = useLauncher((s) => s.renderer);
  const setRenderer = useLauncher((s) => s.setRenderer);
  const launcherName = useLauncher((s) => s.launcherName);
  const setLauncherName = useLauncher((s) => s.setLauncherName);
  const renderers = [
    "Zink (OpenGL 4.6)",
    "VirGL (OpenGL 4.3)",
    "MobileGlues",
    "GL4ES",
  ];

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h2 className="font-display text-2xl font-semibold tracking-wide">
        Launcher settings
      </h2>
      <Panel className="space-y-5">
        <label className="block">
          <span className="text-sm text-muted">Custom launcher name</span>
          <input
            value={launcherName}
            onChange={(e) => setLauncherName(e.target.value)}
            className="mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 outline-none focus:border-accent"
          />
        </label>
        <div>
          <div className="flex justify-between text-sm">
            <span className="text-muted">Memory</span>
            <span className="font-mono tabular-nums">{ram} MB</span>
          </div>
          <input
            type="range"
            min={1024}
            max={8192}
            step={256}
            value={ram}
            onChange={(e) => setRam(Number(e.target.value))}
            className="mt-2 w-full accent-accent"
          />
        </div>
        <label className="block">
          <span className="text-sm text-muted">Renderer</span>
          <select
            value={renderer}
            onChange={(e) => setRenderer(e.target.value)}
            className="mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 outline-none focus:border-accent"
          >
            {renderers.map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
        </label>
      </Panel>
      <Panel className="space-y-2 text-sm leading-relaxed text-muted">
        <p className="font-display text-lg font-semibold tracking-wide text-fg">
          About
        </p>
        <p>
          OrbitX Launcher is a GPL-3.0 fork of Fold Craft Launcher (FCL-Team).
          Maintained by VenZ9. Core from HMCL; runtime from Amethyst-Android /
          PojavLauncher and Boat.
        </p>
        <p className="font-mono text-xs text-subtle">
          com.orbitx.launcher · theme #C0392B · /OrbitX/.minecraft
        </p>
      </Panel>
    </div>
  );
}

function RightDock() {
  const account = useLauncher(selectedAccount);
  const version = useLauncher(selectedVersion);
  const setNav = useLauncher((s) => s.setNav);
  const startLaunch = useLauncher((s) => s.startLaunch);
  const [accountOpen, setAccountOpen] = useState(false);

  return (
    <aside className="relative flex w-full shrink-0 flex-col border-t border-border/80 bg-bg/88 px-4 py-4 backdrop-blur-md md:w-[260px] md:border-t-0 md:border-l">
      <button
        type="button"
        onClick={() => setAccountOpen((v) => !v)}
        className="mx-auto flex flex-col items-center gap-1 py-2"
      >
        <span className="flex size-14 items-center justify-center rounded-full bg-accent/20 text-accent-soft ring-2 ring-accent/40">
          <User className="size-7" />
        </span>
        <span className="font-display text-lg font-semibold tracking-wide">
          {account?.name ?? "No account"}
        </span>
        <span className="text-xs text-subtle">
          {account ? `${account.type} · tap to switch` : "Add account"}
        </span>
      </button>

      {accountOpen && <AccountMenu onClose={() => setAccountOpen(false)} />}

      <button
        type="button"
        onClick={() => setNav("manage")}
        className="mt-auto flex items-center gap-3 rounded-lg border border-border bg-surface-2/80 px-3 py-3 text-left"
      >
        <Blocks className="size-7 shrink-0 text-accent-soft" />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-semibold">
            {version?.name ?? "No version"}
          </span>
          <span className="block truncate text-xs text-subtle">
            {version
              ? `${version.mc}${version.loaderVer ? ` · ${version.loader}` : ""}`
              : "Install a version"}
          </span>
        </span>
        <Settings className="size-4 text-subtle" />
      </button>

      <button
        type="button"
        onClick={startLaunch}
        disabled={!version}
        className="mt-3 flex h-12 items-center justify-center gap-2 rounded-lg bg-accent text-base font-semibold tracking-wide text-fg shadow-[0_8px_24px_rgba(192,57,43,0.35)] transition-transform duration-150 active:scale-[0.98] disabled:opacity-50"
      >
        <Play className="size-5 fill-current" />
        Launch
      </button>
    </aside>
  );
}

function AccountMenu({ onClose }: { onClose: () => void }) {
  const accounts = useLauncher((s) => s.accounts);
  const selected = useLauncher((s) => s.selectedAccountId);
  const selectAccount = useLauncher((s) => s.selectAccount);
  const addOffline = useLauncher((s) => s.addOfflineAccount);
  const [name, setName] = useState("");

  return (
    <div className="absolute inset-x-3 top-36 z-20 rounded-lg border border-border bg-surface p-3 shadow-xl">
      <div className="mb-2 flex items-center justify-between">
        <p className="text-sm font-medium">Accounts</p>
        <button type="button" onClick={onClose} className="size-8 text-subtle">
          <X className="mx-auto size-4" />
        </button>
      </div>
      <ul className="space-y-1">
        {accounts.map((a) => (
          <li key={a.id}>
            <button
              type="button"
              onClick={() => {
                selectAccount(a.id);
                onClose();
              }}
              className={cn(
                "flex h-10 w-full items-center justify-between rounded-md px-2 text-sm",
                selected === a.id ? "bg-accent/20" : "hover:bg-surface-2",
              )}
            >
              <span>{a.name}</span>
              <span className="text-xs text-subtle">{a.type}</span>
            </button>
          </li>
        ))}
      </ul>
      <form
        className="mt-3 flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          addOffline(name);
          setName("");
        }}
      >
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Offline name"
          className="h-10 min-w-0 flex-1 rounded-md border border-border bg-bg px-2 text-sm outline-none focus:border-accent"
        />
        <button
          type="submit"
          className="h-10 rounded-md bg-accent px-3 text-sm font-medium"
        >
          Add
        </button>
      </form>
    </div>
  );
}

function LaunchOverlay() {
  const log = useLauncher((s) => s.launchLog);
  const phase = useLauncher((s) => s.launchPhase);
  const clear = useLauncher((s) => s.clearLaunch);
  const version = useLauncher(selectedVersion);
  const name = useLauncher((s) => s.launcherName);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-bg/80 p-4 backdrop-blur-sm">
      <div className="flex max-h-[min(640px,90dvh)] w-full max-w-2xl flex-col overflow-hidden rounded-xl border border-border bg-surface shadow-2xl">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <div className="flex items-center gap-3">
            <OrbitMark className="size-9" />
            <div>
              <p className="font-display text-lg font-semibold tracking-wide">
                {name}
              </p>
              <p className="text-xs text-subtle">
                Starting {version?.name ?? "game"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={clear}
            className="flex size-10 items-center justify-center rounded-md hover:bg-surface-2"
          >
            <X className="size-5" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto bg-bg px-4 py-3 font-mono text-xs leading-6 text-ok">
          {log.map((line, i) => (
            <p key={i} className="log-line">
              {line}
            </p>
          ))}
        </div>
        {phase === "blocked" && (
          <div className="space-y-3 border-t border-border px-4 py-4">
            <p className="text-sm text-pretty text-muted">
              JVM handoff is native. Install the OrbitX APK on Android to
              actually enter the world — this screen is the new branded chrome.
            </p>
            <button
              type="button"
              onClick={clear}
              className="h-11 w-full rounded-md bg-accent text-sm font-medium"
            >
              Return to launcher
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
