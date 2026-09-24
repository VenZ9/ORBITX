import chromium from "@sparticuz/chromium";
import puppeteer from "puppeteer-core";

const base = process.env.QA_URL ?? "http://127.0.0.1:8080";

const execPath = await chromium.executablePath();
const browser = await puppeteer.launch({
  executablePath: execPath,
  args: [...chromium.args, "--no-sandbox", "--disable-gpu"],
  headless: "shell",
  defaultViewport: { width: 1440, height: 810 },
});

const page = await browser.newPage();
await page.goto(base, { waitUntil: "networkidle0", timeout: 60000 });
await new Promise((r) => setTimeout(r, 1500));

// account menu (the chip inside header > relative div)
await page.click("header div.relative > button");
await new Promise((r) => setTimeout(r, 400));
await page.screenshot({ path: "screenshots/redesign-account.png" });
await page.keyboard.press("Escape");
await page.mouse.click(10, 400);
await new Promise((r) => setTimeout(r, 300));

const tabs = await page.$$("header nav button");
if (tabs[2]) {
  await tabs[2].click();
  await new Promise((r) => setTimeout(r, 600));
  await page.screenshot({ path: "screenshots/redesign-download.png" });
}
if (tabs[1]) {
  await tabs[1].click();
  await new Promise((r) => setTimeout(r, 600));
  await page.screenshot({ path: "screenshots/redesign-instances.png" });
}
if (tabs[4]) {
  await tabs[4].click();
  await new Promise((r) => setTimeout(r, 600));
  await page.screenshot({ path: "screenshots/redesign-multiplayer.png" });
}
if (tabs[5]) {
  await tabs[5].click();
  await new Promise((r) => setTimeout(r, 600));
  await page.screenshot({ path: "screenshots/redesign-settings.png" });
}

// back to play, then launch overlay
if (tabs[0]) {
  await tabs[0].click();
  await new Promise((r) => setTimeout(r, 500));
}
const buttons = await page.$$("button");
for (const b of buttons) {
  const txt = await b.evaluate((el) => el.textContent);
  if (txt && txt.trim() === "PLAY") {
    await b.click();
    break;
  }
}
await new Promise((r) => setTimeout(r, 3200));
await page.screenshot({ path: "screenshots/redesign-launch.png" });

// mobile
const mobile = await browser.newPage();
await mobile.setViewport({ width: 390, height: 844, isMobile: true });
await mobile.goto(base, { waitUntil: "networkidle0", timeout: 60000 });
await new Promise((r) => setTimeout(r, 1200));
await mobile.screenshot({ path: "screenshots/redesign-mobile.png", fullPage: true });

await browser.close();
console.log("QA screenshots written");
