#!/usr/bin/env node
// Generates a synthetic Estonian home loan contract and its standard information sheet (ESIS) as
// PDFs, for demos and tests (TASKS.md T50). Every page says NÄIDIS: these are not real contracts.
// The lender is fictional by default. Uses a locally installed Chromium-based browser to print.
//
//   node tools/synthetic-documents/generate-contract.mjs --name "Mari Maasikas" [options]
//
// See README.md next to this file for the options.

import { execFileSync } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { parseArgs } from 'node:util'
import { pathToFileURL } from 'node:url'

const { values: options } = parseArgs({
  options: {
    name: { type: 'string' },
    code: { type: 'string' },
    'co-borrower': { type: 'string' },
    lender: { type: 'string', default: 'AS Näidispank' },
    number: { type: 'string', default: 'NÄIDIS-2019-0614' },
    date: { type: 'string', default: '2019-06-14' },
    principal: { type: 'string', default: '185000' },
    margin: { type: 'string', default: '1.95' },
    months: { type: 'string', default: '360' },
    'payment-day': { type: 'string', default: '14' },
    'property-value': { type: 'string', default: '231000' },
    address: { type: 'string', default: 'Harju maakond, Tallinn, Näidise tn 1-12' },
    'fixed-rate': { type: 'string' },
    'fixed-until': { type: 'string' },
    out: { type: 'string', default: '.' },
  },
})

if (!options.name) {
  console.error('Usage: generate-contract.mjs --name "First Last" [--code 60001017869] [...]')
  process.exit(1)
}

const principal = Number(options.principal)
const margin = Number(options.margin)
const months = Number(options.months)
const paymentDay = Number(options['payment-day'])
const propertyValue = Number(options['property-value'])
const signed = new Date(`${options.date}T12:00:00Z`)
const fixedRate = options['fixed-rate'] ? Number(options['fixed-rate']) : null

// At signing, 6M Euribor was negative and floored at 0, so the rate was the margin (or the fixed rate)
const rateAtSigning = fixedRate ?? margin
const monthlyRate = rateAtSigning / 100 / 12
const payment =
  monthlyRate === 0
    ? principal / months
    : (principal * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months))
const contractFee = 300
const mortgageAmount = Math.round((principal * 1.2) / 100) * 100
const totalRepayable = payment * months + contractFee
const apr = annualPercentageRate(principal - contractFee, payment, months)

const maturity = addMonths(signed, months)
const firstPayment = addMonths(signed, 1)
firstPayment.setUTCDate(paymentDay)

const euro = (amount, digits = 2) =>
  new Intl.NumberFormat('et-EE', { minimumFractionDigits: digits, maximumFractionDigits: digits })
    .format(amount)
    .replace(/ /g, ' ') + ' eurot'
const percent = (value, digits = 2) =>
  new Intl.NumberFormat('et-EE', { minimumFractionDigits: digits, maximumFractionDigits: 3 }).format(
    value,
  ) + '%'
const date = (d) =>
  `${String(d.getUTCDate()).padStart(2, '0')}.${String(d.getUTCMonth() + 1).padStart(2, '0')}.${d.getUTCFullYear()}`

const borrowers = [options.name, options['co-borrower']].filter(Boolean)
const borrowerRows = borrowers
  .map(
    (name, index) =>
      `<tr><th>${index === 0 ? 'Laenusaaja' : 'Kaaslaenusaaja'}</th><td>${escape(name)}${
        index === 0 && options.code ? `, isikukood ${escape(options.code)}` : ''
      }</td></tr>`,
  )
  .join('')

const interest =
  fixedRate === null
    ? `6 kuu Euribor + ${percent(margin)} aastas. Intressimäära korrigeeritakse iga 6 kuu järel Euribori muutmise päeval. Negatiivse Euribori korral loetakse Euribori määraks 0%.`
    : `${percent(fixedRate)} aastas, fikseeritud kuni ${date(new Date(`${options['fixed-until']}T12:00:00Z`))}; seejärel 6 kuu Euribor + ${percent(margin)} aastas.`

// The watermark is a raster image so it stays out of the text layer that readers extract
const watermarkPng = renderWatermark()

const contract = page(
  `Laenuleping nr ${escape(options.number)}`,
  `
  <p class="meta">Sõlmitud ${date(signed)} Tallinnas</p>
  <h2>1. Pooled</h2>
  <table>
    <tr><th>Laenuandja</th><td>${escape(options.lender)}, registrikood 00000000</td></tr>
    ${borrowerRows}
  </table>
  <h2>2. Põhitingimused</h2>
  <table>
    <tr><th>Laenu eesmärk</th><td>Eluruumi soetamine</td></tr>
    <tr><th>Laenusumma</th><td>${euro(principal)}</td></tr>
    <tr><th>Intressimäär</th><td>${interest}</td></tr>
    <tr><th>Lepingutasu</th><td>${euro(contractFee)}</td></tr>
    <tr><th>Krediidi kulukuse määr</th><td>${percent(apr)} aastas (tüüpnäide)</td></tr>
    <tr><th>Laenugraafiku põhimõte</th><td>Annuiteetmakse: igakuised maksed on kogu perioodi jooksul võrdsed</td></tr>
    <tr><th>Osamaksete arv</th><td>${months} osamakset</td></tr>
    <tr><th>Maksepäev</th><td>Iga kuu ${paymentDay}. kuupäev, alates ${date(firstPayment)}</td></tr>
    <tr><th>Tagastamise tähtpäev</th><td>${date(maturity)}</td></tr>
  </table>
  <h2>3. Tagatis</h2>
  <p>Laenuandja kasuks esimesele järjekohale seatav hüpoteek summas ${euro(mortgageAmount, 0)} korteriomandile aadressil ${escape(options.address)}.</p>
  <h2>4. Ennetähtaegne tagastamine</h2>
  <p>Laenusaajal on õigus laen osaliselt või täielikult ennetähtaegselt tagastada, teatades sellest laenuandjale kirjalikult vähemalt 30 päeva ette. Hüvitis on kuni 3 kuu intressi ulatuses.</p>
  <h2>5. Allkirjad</h2>
  <p>Leping allkirjastatakse digitaalselt.</p>
  <table class="signatures">
    <tr><td>${escape(options.lender)}</td>${borrowers.map((name) => `<td>${escape(name)}</td>`).join('')}</tr>
  </table>
`,
)

const esis = page(
  'Euroopa tarbijakrediidi standardinfo teabeleht',
  `
  <p class="meta">Koostatud ${date(signed)} laenulepingu nr ${escape(options.number)} juurde</p>
  <table>
    <tr><th>Laenuandja</th><td>${escape(options.lender)}</td></tr>
    <tr><th>Laenu summa ja vääring</th><td>${euro(principal)}</td></tr>
    <tr><th>Kestus</th><td>${months} kuud</td></tr>
    <tr><th>Kohaldatava intressimäära liik</th><td>${fixedRate === null ? `fikseerimata, 6 kuu Euribor + ${percent(margin)} aastas` : `fikseeritud ${percent(fixedRate)} kuni ${date(new Date(`${options['fixed-until']}T12:00:00Z`))}, seejärel 6 kuu Euribor + ${percent(margin)} aastas`}</td></tr>
    <tr><th>Igakuise makse summa</th><td>${euro(payment)}</td></tr>
    <tr><th>Kogu tagasimakstav summa</th><td>${euro(totalRepayable)} (näitlik)</td></tr>
    <tr><th>Krediidi kulukuse määr</th><td>${percent(apr)} aastas</td></tr>
    <tr><th>Nõutav tagatis</th><td>Hüpoteek summas vähemalt ${euro(mortgageAmount, 0)}</td></tr>
    <tr><th>Kinnisvara eeldatav väärtus</th><td>${euro(propertyValue, 0)}</td></tr>
  </table>
`,
)

const outDir = resolve(options.out)
mkdirSync(outDir, { recursive: true })
const slug = slugify(options.name)
const written = [
  print(contract, join(outDir, `${slug}-laenuleping.pdf`)),
  print(esis, join(outDir, `${slug}-standardinfo-teabeleht.pdf`)),
]
console.log(`Monthly payment ${payment.toFixed(2)}, APR ${apr.toFixed(3)}%, maturity ${date(maturity)}`)
written.forEach((file) => console.log(`Wrote ${file}`))

function page(title, body) {
  return `<!doctype html>
<html lang="et">
<head>
<meta charset="utf-8">
<title>${escape(title)}</title>
<style>
  @page { size: A4; margin: 22mm 20mm 24mm; }
  body { font-family: Helvetica, Arial, sans-serif; font-size: 10.5pt; color: #1a1a1a; line-height: 1.45; }
  h1 { font-size: 16pt; margin: 0 0 4pt; }
  h2 { font-size: 11pt; margin: 16pt 0 6pt; }
  .meta { color: #555; margin: 0 0 12pt; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: left; vertical-align: top; padding: 5pt 6pt; border-bottom: 0.5pt solid #ccc; }
  th { width: 34%; font-weight: 600; }
  .signatures td { border: none; padding-top: 28pt; border-top: 0.5pt solid #999; }
  .watermark { position: fixed; top: 30%; left: 0; right: 0; height: 120mm; background: url("data:image/png;base64,${watermarkPng}") center / contain no-repeat; }
  .notice { color: #b00; font-size: 9.5pt; margin: 0 0 14pt; padding: 4pt 0; border-top: 0.5pt solid #b00; border-bottom: 0.5pt solid #b00; }
</style>
</head>
<body>
<div class="watermark"></div>
<h1>${escape(title)}</h1>
<p class="notice">NÄIDIS – ei ole päris laenuleping. Loodud ühispakkumise demo jaoks.</p>
${body}
</body>
</html>`
}

function renderWatermark() {
  const work = mkdtempSync(join(tmpdir(), 'synthetic-watermark-'))
  const svgFile = join(work, 'watermark.svg')
  const pngFile = join(work, 'watermark.png')
  writeFileSync(
    svgFile,
    `<svg xmlns='http://www.w3.org/2000/svg' width='600' height='300' viewBox='0 0 600 300'><text x='300' y='190' text-anchor='middle' font-family='Helvetica, Arial' font-size='120' font-weight='700' fill='rgb(176,0,0)' fill-opacity='0.08' transform='rotate(-25 300 150)'>NÄIDIS</text></svg>`,
  )
  try {
    execFileSync(
      browser(),
      [
        '--headless=new',
        '--disable-gpu',
        '--hide-scrollbars',
        '--default-background-color=00000000',
        '--window-size=600,300',
        `--screenshot=${pngFile}`,
        pathToFileURL(svgFile).href,
      ],
      { stdio: 'ignore' },
    )
    return readFileSync(pngFile).toString('base64')
  } finally {
    rmSync(work, { recursive: true, force: true })
  }
}

function print(html, target) {
  const work = mkdtempSync(join(tmpdir(), 'synthetic-contract-'))
  const htmlFile = join(work, 'document.html')
  writeFileSync(htmlFile, html)
  try {
    execFileSync(browser(), [
      '--headless=new',
      '--disable-gpu',
      '--no-pdf-header-footer',
      `--print-to-pdf=${target}`,
      pathToFileURL(htmlFile).href,
    ], { stdio: 'ignore' })
  } finally {
    rmSync(work, { recursive: true, force: true })
  }
  return target
}

function browser() {
  const candidates = [
    process.env.BROWSER,
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
  ].filter(Boolean)
  const found = candidates.find((path) => existsSync(path))
  if (!found) {
    throw new Error('No Chromium-based browser found; set BROWSER to its executable')
  }
  return found
}

function annualPercentageRate(netAmount, monthlyPayment, count) {
  let low = 0
  let high = 0.05
  for (let i = 0; i < 100; i++) {
    const mid = (low + high) / 2
    const presentValue = (monthlyPayment * (1 - Math.pow(1 + mid, -count))) / mid
    if (presentValue > netAmount) {
      low = mid
    } else {
      high = mid
    }
  }
  return (Math.pow(1 + (low + high) / 2, 12) - 1) * 100
}

function addMonths(from, count) {
  const result = new Date(from)
  result.setUTCMonth(result.getUTCMonth() + count)
  return result
}

function slugify(text) {
  return text
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

function escape(text) {
  return String(text).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])
}
