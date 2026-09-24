import { useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import {
  Boxes,
  Check,
  ChevronDown,
  ChevronRight,
  Coffee,
  Cpu,
  Download,
  ExternalLink,
  FolderOpen,
  Gamepad2,
  Layers,
  Play,
  Plus,
  Settings,
  Share2,
  Trash2,
  Wrench,
  X,
  Zap,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  CATALOG,
  selectedAccount,
  selectedVersion,
  useLauncher,
  type Loader,
  type NavId,
  type Version,
} from "@/lib/launcher-store";
import { OrbitMark } from "./orbit-mark";
import { PlayerHead, PlayerPreview } from "./player-preview";

const NAV: { id: NavId; label: string; icon: typeof Play }[] = [
  { id: "home", label: "Play", icon: Play },
  { id: "manage", label: "Instances", icon: Layers },
  { id: "download", label: "Download", icon: Download },
  { id: "controller", label: "Controls", icon: Gamepad2 },
  { id: "multiplayer", label: "Multiplayer", icon: Share2 },
  { id: "settings", label: "Settings", icon: Settings },
];

export function LauncherShell() {
  const nav = useLauncher((s) => s.nav);
  const launching = useLauncher((s) => s.launching);

  return (
    <div className="relative min-h-dvh overflow-hidden bg-bg text-fg">
      {/* world backdrop + ember vignette */}
      <div
        className="pointer-events-none absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: "url(/launcher-bg.jpg)" }}
      />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(120%_90%_at_85%_100%,rgba(122,19,13,0.5),transparent_55%)]" />
      <div className="pointer-events-none absolute inset-0 bg-linear-to-b from-bg/80 via-bg/55 to-bg/85" />

      <div className="relative z-10 flex min-h-dvh flex-col">
        <TopBar />
        <main className="mx-auto w-full max-w-[1280px] flex-1 px-3 pt-4 pb-2 md:px-6">
          {nav === "home" && <HomeGrid />}
          {nav === "manage" && <ManageView />}
          {nav === "download" && <DownloadView />}
          {nav === "controller" && <ControllerView />}
          {nav === "multiplayer" && <MultiplayerView />}
          {nav === "settings" && <SettingsView />}
        </main>
        <Footer />
      </div>

      {launching && <LaunchOverlay />}
    </div>
  );
}

/* ───────────────────────────── top bar ─────────────────────────────── */

function TopBar() {
  const nav = useLauncher((s) => s.nav);
  const setNav = useLauncher((s) => s.setNav);
  const name = useLauncher((s) => s.launcherName);
  const account = useLauncher(selectedAccount);
  const [accountOpen, setAccountOpen] = useState(false);

  return (
    <header className="sticky top-0 z-30 border-b border-white/6 bg-bg/72 backdrop-blur-xl">
      <div className="mx-auto flex h-15 max-w-[1280px] items-center gap-2 px-3 md:gap-4 md:px-6">
        {/* brand */}
        <button
          type="button"
          onClick={() => setNav("home")}
          className="flex shrink-0 items-center gap-2.5"
          title={name}
        >
          <OrbitMark className="size-8" />
          <span className="hidden flex-col items-start leading-none sm:flex">
            <span className="font-display text-[1.35rem] font-bold tracking-[0.08em]">
              ORBITX
            </span>
            <span className="text-[10px] font-medium tracking-[0.28em] text-subtle">
              LAUNCHER
            </span>
          </span>
        </button>

        {/* tabs */}
        <nav
          className="flex min-w-0 flex-1 items-center gap-0.5 overflow-x-auto px-1 md:justify-center"
          aria-label="Launcher"
        >
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = nav === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setNav(item.id)}
                data-active={active}
                className={cn(
                  "fc-tab flex h-15 shrink-0 items-center gap-1.5 px-2.5 text-[13px] font-medium whitespace-nowrap transition-colors md:px-3.5",
                  active ? "text-fg" : "text-subtle hover:text-muted",
                )}
                aria-current={active ? "page" : undefined}
              >
                <Icon className="size-4" strokeWidth={1.9} />
                <span className="hidden lg:inline">{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* account chip */}
        <div className="relative shrink-0">
          <button
            type="button"
            onClick={() => setAccountOpen((v) => !v)}
            className={cn(
              "flex h-10 items-center gap-2 rounded-xl border px-1.5 pr-2 transition-colors",
              accountOpen
                ? "border-accent/50 bg-accent/12"
                : "border-white/8 bg-surface-2/80 hover:border-accent/40",
            )}
          >
            <PlayerHead className="size-7 rounded-md" />
            <span className="hidden max-w-24 truncate text-[13px] font-medium sm:block">
              {account?.name ?? "Offline"}
            </span>
            <ChevronDown
              className={cn(
                "size-3.5 text-subtle transition-transform",
                accountOpen && "rotate-180",
              )}
            />
          </button>
          {accountOpen && <AccountMenu onClose={() => setAccountOpen(false)} />}
        </div>
      </div>
    </header>
  );
}

function AccountMenu({ onClose }: { onClose: () => void }) {
  const accounts = useLauncher((s) => s.accounts);
  const selected = useLauncher((s) => s.selectedAccountId);
  const selectAccount = useLauncher((s) => s.selectAccount);
  const addOffline = useLauncher((s) => s.addOfflineAccount);
  const [name, setName] = useState("");

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  // Portal to <body>: the header's backdrop-blur makes it the containing
  // block for fixed descendants, which would shrink this menu's scrim to
  // the header strip and swallow every click on the page behind it.
  return createPortal(
    <>
      <button
        type="button"
        className="fixed inset-0 z-40 cursor-default"
        onClick={onClose}
        aria-label="Close account menu"
      />
      <div className="fc-card fixed top-[3.75rem] right-3 z-50 w-72 p-2 md:right-6">
        <p className="px-2 pt-1 pb-2 text-[11px] font-semibold tracking-[0.2em] text-subtle uppercase">
          Accounts
        </p>
        <ul className="space-y-0.5">
          {accounts.map((a) => (
            <li key={a.id}>
              <button
                type="button"
                onClick={() => {
                  selectAccount(a.id);
                  onClose();
                }}
                className={cn(
                  "flex h-11 w-full items-center gap-2.5 rounded-xl px-2 text-sm transition-colors",
                  selected === a.id
                    ? "bg-accent/14 text-fg"
                    : "text-muted hover:bg-white/5 hover:text-fg",
                )}
              >
                <PlayerHead className="size-7 rounded-md" />
                <span className="min-w-0 flex-1 truncate text-left">{a.name}</span>
                <span className="text-[11px] text-subtle">{a.type}</span>
                {selected === a.id && (
                  <Check className="size-4 text-accent-soft" />
                )}
              </button>
            </li>
          ))}
        </ul>
        <form
          className="mt-2 flex gap-1.5 border-t border-white/6 pt-2"
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
            className="h-10 min-w-0 flex-1 rounded-lg border border-white/8 bg-bg px-2.5 text-sm outline-none placeholder:text-subtle focus:border-accent/60"
          />
          <button
            type="submit"
            className="fc-play relative flex h-10 items-center gap-1 overflow-hidden rounded-lg px-3 text-sm font-semibold"
          >
            <Plus className="size-4" />
            Add
          </button>
        </form>
      </div>
    </>,
    document.body,
  );
}

/* ─────────────────────────── home: hero + rail ─────────────────────── */

function HomeGrid() {
  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
      <HeroCard />
      <InstanceRail />
    </div>
  );
}

function SpecPill({
  icon,
  children,
  tint,
}: {
  icon: ReactNode;
  children: ReactNode;
  tint: "ember" | "blue" | "gold" | "neutral";
}) {
  return (
    <span
      className={cn(
        "inline-flex h-8 items-center gap-1.5 rounded-[10px] px-2.5 text-[12.5px] font-medium",
        tint === "ember" && "pill-ember",
        tint === "blue" && "pill-blue",
        tint === "gold" && "pill-gold",
        tint === "neutral" && "pill-neutral",
      )}
    >
      {icon}
      {children}
    </span>
  );
}

function HeroCard() {
  const account = useLauncher(selectedAccount);
  const version = useLauncher(selectedVersion);
  const ramMb = useLauncher((s) => s.ramMb);
  const setNav = useLauncher((s) => s.setNav);
  const startLaunch = useLauncher((s) => s.startLaunch);
  const online = account?.type === "Microsoft";

  return (
    <section className="fc-card flex min-h-[560px] flex-col overflow-hidden lg:h-[calc(100dvh-7.5rem)] lg:min-h-0">
      <div className="fc-rule" />
      <div className="flex min-h-0 flex-1 flex-col p-4 md:p-6">
        {/* identity */}
        <div className="flex items-center gap-4">
          <div className="relative shrink-0">
            <PlayerHead className="size-16 rounded-2xl md:size-18" framed />
            <span
              className={cn(
                "absolute -right-1 -bottom-1 size-4 rounded-full border-[3px] border-[#101116]",
                online ? "status-dot bg-ok" : "bg-subtle",
              )}
              title={online ? "Online" : "Offline"}
            />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <h1 className="font-display truncate text-3xl leading-none font-bold tracking-wide md:text-4xl">
                {account?.name ?? "Player"}
              </h1>
              <span
                className={cn(
                  "inline-flex h-6 items-center rounded-md px-2 text-[11px] font-semibold tracking-wide",
                  online ? "pill-ok" : "pill-neutral",
                )}
              >
                {online ? "Online" : "Offline"}
              </span>
            </div>
            <button
              type="button"
              onClick={() => setNav("manage")}
              className="mt-1.5 flex items-center gap-1.5 text-sm text-subtle transition-colors hover:text-muted"
            >
              <FolderOpen className="size-4" />
              <span>
                Profile:{" "}
                <span className="font-semibold text-fg">
                  {version?.name ?? "none"}
                </span>
              </span>
              <ChevronRight className="size-3.5" />
            </button>
          </div>
        </div>

        {/* spec pills — one hue, one meaning */}
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <SpecPill tint="ember" icon={<Boxes className="size-4" />}>
            {version?.mc ?? "—"}
          </SpecPill>
          <SpecPill tint="blue" icon={<Layers className="size-4" />}>
            {version?.loader ?? "Vanilla"}
          </SpecPill>
          <SpecPill tint="gold" icon={<Coffee className="size-4" />}>
            Java 21+
          </SpecPill>
          <SpecPill tint="neutral" icon={<Cpu className="size-4" />}>
            {(ramMb / 1024).toFixed(1)}GB
          </SpecPill>
        </div>

        {/* stage */}
        <div className="relative my-2 min-h-56 flex-1">
          <PlayerPreview name={account?.name ?? "Orbit"} />
        </div>

        {/* the action */}
        <button
          type="button"
          onClick={startLaunch}
          disabled={!version}
          className="fc-play relative flex h-16 w-full shrink-0 items-center justify-center gap-3 overflow-hidden rounded-2xl transition-[filter,transform] duration-150"
        >
          <Play className="size-6 fill-current" />
          <span className="font-display text-2xl font-bold tracking-[0.35em]">
            PLAY
          </span>
        </button>
      </div>
    </section>
  );
}

function loaderTint(loader: Loader) {
  switch (loader) {
    case "Fabric":
      return "bg-[rgba(58,144,255,0.16)] text-pill-blue";
    case "Forge":
      return "bg-[rgba(255,176,60,0.14)] text-pill-gold";
    case "Quilt":
      return "bg-[rgba(61,220,132,0.14)] text-ok";
    case "NeoForge":
      return "bg-[rgba(255,90,60,0.16)] text-accent-soft";
    default:
      return "bg-white/6 text-muted";
  }
}

function InstanceRail() {
  const versions = useLauncher((s) => s.versions);
  const selectedId = useLauncher((s) => s.selectedVersionId);
  const selectVersion = useLauncher((s) => s.selectVersion);
  const setNav = useLauncher((s) => s.setNav);

  return (
    <aside className="flex min-h-0 flex-col gap-2.5 lg:h-[calc(100dvh-7.5rem)]">
      {/* quick card */}
      <button
        type="button"
        onClick={() => setNav("download")}
        className="fc-card fc-shimmer relative flex items-center gap-3 overflow-hidden p-3 text-left transition-colors hover:border-accent/40"
      >
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-accent/14 text-accent-soft">
          <Download className="size-5" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-sm font-semibold">Download Center</span>
          <span className="block text-xs text-subtle">
            Versions, loaders & mods
          </span>
        </span>
        <ChevronRight className="size-4 shrink-0 text-subtle" />
      </button>

      {/* instance list */}
      <div className="flex items-center justify-between px-1">
        <p className="text-[11px] font-semibold tracking-[0.22em] text-subtle uppercase">
          Instances
        </p>
        <span className="font-mono text-[11px] text-subtle">
          {versions.length}
        </span>
      </div>

      <div className="min-h-0 flex-1 space-y-2 overflow-y-auto pr-0.5 lg:overflow-visible">
        {versions.map((v, i) => {
          const active = v.id === selectedId;
          return (
            <button
              key={v.id}
              type="button"
              onClick={() => selectVersion(v.id)}
              className={cn(
                "relative flex w-full items-center gap-3 rounded-2xl border p-2.5 text-left transition-all",
                active
                  ? "border-accent/60 bg-accent/10 shadow-[0_8px_28px_rgba(227,55,43,0.18)]"
                  : "border-white/7 bg-surface/85 hover:border-white/16",
              )}
            >
              <span
                className={cn(
                  "flex size-11 shrink-0 items-center justify-center rounded-xl",
                  loaderTint(v.loader),
                )}
              >
                <Boxes className="size-5.5" strokeWidth={1.8} />
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold">
                  {v.name}
                </span>
                <span className="block truncate text-xs text-subtle">
                  {v.lastPlayed ? `Played ${v.lastPlayed.toLowerCase()}` : v.loader}
                </span>
              </span>
              {active ? (
                <span className="flex size-5.5 items-center justify-center rounded-full bg-accent">
                  <Check className="size-3.5 text-white" strokeWidth={3} />
                </span>
              ) : (
                <span className="font-display text-sm font-bold text-subtle/60">
                  #{i + 1}
                </span>
              )}
            </button>
          );
        })}
      </div>

      <button
        type="button"
        onClick={() => setNav("manage")}
        className="flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl border border-white/8 bg-surface-2/70 text-sm font-medium text-muted transition-colors hover:border-accent/40 hover:text-fg"
      >
        <Wrench className="size-4" />
        Manage all
      </button>
    </aside>
  );
}

/* ───────────────────────── shared view pieces ──────────────────────── */

function ViewHeader({
  icon,
  title,
  sub,
  action,
}: {
  icon: ReactNode;
  title: string;
  sub: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div className="flex items-center gap-3">
        <span className="flex size-11 items-center justify-center rounded-2xl border border-white/8 bg-surface-2/80 text-accent-soft">
          {icon}
        </span>
        <div>
          <h2 className="font-display text-2xl font-bold tracking-wide">
            {title}
          </h2>
          <p className="text-sm text-subtle">{sub}</p>
        </div>
      </div>
      {action}
    </div>
  );
}

function PrimaryButton({
  children,
  onClick,
  className,
  disabled,
  type = "button",
}: {
  children: ReactNode;
  onClick?: () => void;
  className?: string;
  disabled?: boolean;
  type?: "button" | "submit";
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "fc-play relative inline-flex h-11 items-center justify-center gap-2 overflow-hidden rounded-xl px-4 text-sm font-semibold",
        className,
      )}
    >
      {children}
    </button>
  );
}

/* ────────────────────────────── instances ──────────────────────────── */

function ManageView() {
  const versions = useLauncher((s) => s.versions);
  const selectedId = useLauncher((s) => s.selectedVersionId);
  const selectVersion = useLauncher((s) => s.selectVersion);
  const removeVersion = useLauncher((s) => s.removeVersion);
  const setNav = useLauncher((s) => s.setNav);

  return (
    <div>
      <ViewHeader
        icon={<Layers className="size-5.5" />}
        title="Instances"
        sub="Installed profiles on this device"
        action={
          <PrimaryButton onClick={() => setNav("download")}>
            <Plus className="size-4" />
            Install
          </PrimaryButton>
        }
      />
      <ul className="space-y-2">
        {versions.map((v) => {
          const active = v.id === selectedId;
          return (
            <li
              key={v.id}
              className={cn(
                "flex items-center gap-3 rounded-2xl border p-3 transition-colors",
                active
                  ? "border-accent/60 bg-accent/10"
                  : "border-white/7 bg-surface/85 hover:border-white/16",
              )}
            >
              <button
                type="button"
                onClick={() => selectVersion(v.id)}
                className="flex min-w-0 flex-1 items-center gap-3 text-left"
              >
                <span
                  className={cn(
                    "flex size-11 shrink-0 items-center justify-center rounded-xl",
                    loaderTint(v.loader),
                  )}
                >
                  <Boxes className="size-5.5" strokeWidth={1.8} />
                </span>
                <span className="min-w-0">
                  <span className="block truncate font-semibold">{v.name}</span>
                  <span className="block truncate text-xs text-subtle">
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
                className="flex size-10 shrink-0 items-center justify-center rounded-xl text-subtle transition-colors hover:bg-accent/12 hover:text-accent-soft"
                title="Remove"
              >
                <Trash2 className="size-4" />
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/* ────────────────────────────── download ───────────────────────────── */

function DownloadView() {
  const [loader, setLoader] = useState<Loader>("Vanilla");
  const [busy, setBusy] = useState<string | null>(null);
  const installVersion = useLauncher((s) => s.installVersion);
  const versions = useLauncher((s) => s.versions);
  const loaders: Loader[] = ["Vanilla", "Fabric", "Forge", "Quilt", "NeoForge"];

  function install(mc: string, type: "release" | "snapshot") {
    const id = loader === "Vanilla" ? mc : `${mc}-${loader.toLowerCase()}`;
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
    <div className="mx-auto max-w-3xl">
      <ViewHeader
        icon={<Download className="size-5.5" />}
        title="Download Center"
        sub="Install a Minecraft version. Loaders apply at install time."
      />
      <div className="mb-4 flex flex-wrap gap-1.5 rounded-2xl border border-white/7 bg-surface/85 p-1.5">
        {loaders.map((l) => (
          <button
            key={l}
            type="button"
            onClick={() => setLoader(l)}
            className={cn(
              "h-9 flex-1 rounded-xl px-3 text-sm font-medium whitespace-nowrap transition-colors",
              loader === l
                ? "bg-accent text-white shadow-[0_4px_16px_rgba(227,55,43,0.35)]"
                : "text-subtle hover:text-fg",
            )}
          >
            {l}
          </button>
        ))}
      </div>
      <ul className="fc-card divide-y divide-white/6 overflow-hidden">
        {CATALOG.map((item) => {
          const id = loader === "Vanilla" ? item.mc : `${item.mc}-${loader.toLowerCase()}`;
          const have = versions.some((v) => v.id === id);
          return (
            <li key={item.id + loader} className="flex items-center gap-3 p-3.5">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/5 text-subtle">
                <Boxes className="size-5" strokeWidth={1.8} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-semibold">{item.mc}</p>
                <p className="text-xs text-subtle">
                  {item.type} · {item.date}
                </p>
              </div>
              <PrimaryButton
                disabled={have || busy === id}
                onClick={() => install(item.mc, item.type)}
                className={cn(
                  "min-w-28",
                  have && "bg-surface-3 text-subtle shadow-none grayscale-0",
                )}
              >
                {have ? (
                  "Installed"
                ) : busy === id ? (
                  "Installing…"
                ) : (
                  <>
                    <Download className="size-4" />
                    Install
                  </>
                )}
              </PrimaryButton>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/* ────────────────────────────── controls ───────────────────────────── */

function ControllerView() {
  const layouts = useLauncher((s) => s.layouts);
  const selected = useLauncher((s) => s.selectedLayoutId);
  const selectLayout = useLauncher((s) => s.selectLayout);
  return (
    <div className="mx-auto max-w-3xl">
      <ViewHeader
        icon={<Gamepad2 className="size-5.5" />}
        title="Control layouts"
        sub="Touch mappings used in-game. Compatible with ZalithLauncher2 import."
      />
      <ul className="grid gap-3 sm:grid-cols-2">
        {layouts.map((l) => {
          const active = l.id === selected;
          return (
            <li key={l.id}>
              <button
                type="button"
                onClick={() => selectLayout(l.id)}
                className={cn(
                  "flex h-full w-full flex-col items-start rounded-2xl border p-4 text-left transition-colors",
                  active
                    ? "border-accent/60 bg-accent/10"
                    : "border-white/7 bg-surface/85 hover:border-white/16",
                )}
              >
                <span className="flex items-center gap-2.5 font-semibold">
                  <span
                    className={cn(
                      "flex size-9 items-center justify-center rounded-xl",
                      active ? "bg-accent/18 text-accent-soft" : "bg-white/5 text-subtle",
                    )}
                  >
                    <Gamepad2 className="size-4.5" />
                  </span>
                  {l.name}
                  {active && <Check className="size-4 text-accent-soft" />}
                </span>
                <span className="mt-2.5 text-sm text-subtle">
                  {l.author} · {l.buttons} buttons
                </span>
                {active && (
                  <span className="mt-3 text-[11px] font-semibold tracking-[0.18em] text-accent-soft uppercase">
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

/* ───────────────────────────── multiplayer ─────────────────────────── */

function MultiplayerView() {
  const [mode, setMode] = useState<"host" | "guest">("host");
  const [code] = useState("ORBIT-7K2Q");
  const [join, setJoin] = useState("");
  const [status, setStatus] = useState("");

  return (
    <div className="mx-auto max-w-xl">
      <ViewHeader
        icon={<Share2 className="size-5.5" />}
        title="Multiplayer"
        sub="Terracotta mesh — play LAN across the internet, including offline accounts."
      />
      <div className="mb-3 flex gap-1.5 rounded-2xl border border-white/7 bg-surface/85 p-1.5">
        {(["host", "guest"] as const).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => {
              setMode(m);
              setStatus("");
            }}
            className={cn(
              "h-10 flex-1 rounded-xl text-sm font-medium capitalize transition-colors",
              mode === m
                ? "bg-accent text-white shadow-[0_4px_16px_rgba(227,55,43,0.35)]"
                : "text-subtle hover:text-fg",
            )}
          >
            {m}
          </button>
        ))}
      </div>
      <div className="fc-card p-5">
        {mode === "host" ? (
          <div className="space-y-4">
            <p className="text-sm text-muted">
              Open to LAN in-game, then share this invite code.
            </p>
            <p className="rounded-xl border border-white/7 bg-bg py-4 text-center font-mono text-2xl tracking-[0.2em]">
              {code}
            </p>
            <PrimaryButton
              className="w-full"
              onClick={() => {
                void navigator.clipboard?.writeText(code);
                setStatus("Invite code copied");
              }}
            >
              <ExternalLink className="size-4" />
              Copy invite code
            </PrimaryButton>
          </div>
        ) : (
          <form
            className="space-y-4"
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
                className="mt-1.5 h-11 w-full rounded-xl border border-white/8 bg-bg px-3 font-mono tracking-wider text-fg outline-none placeholder:text-subtle focus:border-accent/60"
                placeholder="ORBIT-XXXX"
              />
            </label>
            <PrimaryButton type="submit" className="w-full">
              Join session
            </PrimaryButton>
          </form>
        )}
        {status && <p className="mt-3 text-sm text-accent-soft">{status}</p>}
      </div>
    </div>
  );
}

/* ────────────────────────────── settings ───────────────────────────── */

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
    <div className="mx-auto max-w-2xl">
      <ViewHeader
        icon={<Settings className="size-5.5" />}
        title="Settings"
        sub="Launcher behaviour and performance"
      />
      <div className="fc-card space-y-6 p-5">
        <label className="block">
          <span className="text-sm text-muted">Custom launcher name</span>
          <input
            value={launcherName}
            onChange={(e) => setLauncherName(e.target.value)}
            className="mt-1.5 h-11 w-full rounded-xl border border-white/8 bg-bg px-3 outline-none focus:border-accent/60"
          />
        </label>
        <div>
          <div className="flex justify-between text-sm">
            <span className="text-muted">Memory</span>
            <span className="font-mono text-pill-gold tabular-nums">{ram} MB</span>
          </div>
          <input
            type="range"
            min={1024}
            max={8192}
            step={256}
            value={ram}
            onChange={(e) => setRam(Number(e.target.value))}
            className="mt-2.5 w-full accent-accent"
          />
        </div>
        <label className="block">
          <span className="text-sm text-muted">Renderer</span>
          <select
            value={renderer}
            onChange={(e) => setRenderer(e.target.value)}
            className="mt-1.5 h-11 w-full rounded-xl border border-white/8 bg-bg px-3 outline-none focus:border-accent/60"
          >
            {renderers.map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
        </label>
      </div>
      <div className="fc-card mt-3 space-y-2 p-5 text-sm leading-relaxed text-muted">
        <p className="font-display text-lg font-bold tracking-wide text-fg">
          About
        </p>
        <p>
          OrbitX Launcher is a GPL-3.0 fork of Fold Craft Launcher (FCL-Team).
          Maintained by VenZ9. Core from HMCL; runtime from Amethyst-Android /
          PojavLauncher and Boat.
        </p>
        <p className="font-mono text-xs text-subtle">
          com.orbitx.launcher · theme #E3372B · /OrbitX/.minecraft
        </p>
      </div>
    </div>
  );
}

/* ──────────────────────────── launch overlay ───────────────────────── */

function LaunchOverlay() {
  const log = useLauncher((s) => s.launchLog);
  const phase = useLauncher((s) => s.launchPhase);
  const clear = useLauncher((s) => s.clearLaunch);
  const version = useLauncher(selectedVersion);
  const name = useLauncher((s) => s.launcherName);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-bg/80 p-4 backdrop-blur-sm">
      <div className="fc-card flex max-h-[min(640px,90dvh)] w-full max-w-2xl flex-col overflow-hidden">
        <div className="fc-rule" />
        <div className="flex items-center justify-between border-b border-white/6 px-5 py-3.5">
          <div className="flex items-center gap-3">
            <OrbitMark className="size-9" />
            <div>
              <p className="font-display text-lg font-bold tracking-wide">
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
            className="flex size-10 items-center justify-center rounded-xl transition-colors hover:bg-white/6"
          >
            <X className="size-5" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto bg-black/40 px-5 py-3 font-mono text-xs leading-6 text-ok">
          {log.map((line, i) => (
            <p key={i} className="log-line">
              {line}
            </p>
          ))}
        </div>
        {phase === "blocked" && (
          <div className="space-y-3 border-t border-white/6 px-5 py-4">
            <p className="text-sm text-pretty text-muted">
              JVM handoff is native. Install the OrbitX APK on Android to
              actually enter the world — this screen is the branded chrome.
            </p>
            <PrimaryButton className="w-full" onClick={clear}>
              <Zap className="size-4" />
              Return to launcher
            </PrimaryButton>
          </div>
        )}
      </div>
    </div>
  );
}

/* ─────────────────────────────── footer ────────────────────────────── */

function Footer() {
  const name = useLauncher((s) => s.launcherName);
  return (
    <footer className="relative z-10 py-3 text-center text-[11px] tracking-wide text-subtle/80">
      {name} v3.2 · GPL-3.0 · a Fold Craft Launcher fork · built for Android
    </footer>
  );
}

export type { Version };
