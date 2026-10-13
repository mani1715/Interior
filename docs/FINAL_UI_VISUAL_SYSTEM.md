# ELÉGANCE — CANONICAL UI VISUAL SYSTEM SPECIFICATION
**Product:** Elégance — Interior Designer Platform  
**Status:** RELEASE CANDIDATE LOCKED  
**Baseline Canvas Invariant:** `#FAF8F5` (Warm Cream / Warm Ivory) | Homepage Exception: `#FAF8F3`

---

## 1. Palette Invariants & Design Tokens

The Elégance design language conveys architectural restraint, editorial sophistication, warmth, and calm luxury. It avoids cold blue/grey SaaS gradients, dark dashboards, neon accents, and heavy glassmorphism.

### 1.1 Color Palette Tokens

```css
:root {
  /* Canonical Canvas */
  --page-background: #FAF8F5;       /* Warm Ivory (Primary Application Canvas) */
  --surface-primary: #FFFFFF;       /* Pure White (Cards, Modals, Forms, Drawers) */
  --surface-subtle: #F4F4F4;        /* Light Grey (Secondary / Raised Surface) */
  --surface-sunken: #FAF8F5;        /* Sunken / Inset Workspace Canvas */

  /* Hairline Borders */
  --border-subtle: #E7E1D8;         /* Sage Neutral Hairline Border */
  --border: #D9D9D9;                /* Medium Grey Neutral */
  --border-strong: #6B6B6B;         /* Dark Grey Accessible Border */

  /* Typography / Inks */
  --text-primary: #1F1F1F;          /* Deep Charcoal (Primary Invariant) */
  --text-secondary: #4A4A4A;        /* Charcoal Secondary */
  --text-muted: #6B6B6B;            /* Dark Grey (Muted Metadata / Captions) */
  --text-inverse: #FFFFFF;          /* High-Contrast White */

  /* Editorial Accents */
  --accent-bronze: #B88A5A;         /* Warm Bronze (Brand CTA / Focus / Accent) */
  --brand-hover: #A3784A;           /* Warm Bronze Darkened */
  --brand-active: #8E653B;          /* Deep Bronze */
  --brand-muted: rgba(184, 138, 90, 0.12); /* Subtle Bronze Tint */

  /* Semantic Status Colors (Brand Mapped) */
  --success: #2E5D4B;               /* Forest Green */
  --success-surface: #EDF5F1;
  --warning: #C76F4A;               /* Terracotta */
  --warning-surface: #FAF0EC;
  --danger: #B33A3A;                /* Restrained Crimson */
  --danger-surface: #FDF2F2;
  --info: #3E6D8C;                  /* Muted Blue */
  --info-surface: #EFF5F9;
}
```

---

## 2. Visual Hierarchy Rules

### 2.1 The Cream Canvas Hierarchy
* **Page Canvas:** `#FAF8F5` (or `#FAF8F3` on the homepage). The entire background must never be washed out in pure cold white or dark SaaS slate.
* **Content Surfaces / Cards:** `#FFFFFF` with hairline `#E7E1D8` (`border-sand-200`) borders.
* **Text:** `#1F1F1F` (`text-charcoal-900`) for headlines, `#6B6B6B` (`text-charcoal-500`) for metadata.
* **Bronze Accents:** Used sparingly as focal points (primary CTA buttons, active tab indicators, selected item badges).

---

## 3. Component System Treatments

### 3.1 Buttons
* **Primary Button:** `bg-charcoal-900 hover:bg-black text-white` OR `bg-bronze-700 hover:bg-bronze-800 text-white`. Min-height 44px on mobile touch targets, `rounded-xl`, font-weight medium (`font-medium text-xs sm:text-sm`).
* **Secondary Button:** `bg-white border border-sand-300 text-charcoal-800 hover:bg-sand-50 rounded-xl`.
* **Tertiary / Ghost Button:** `text-charcoal-600 hover:text-charcoal-900 hover:bg-sand-100 rounded-lg`.
* **Destructive Button:** `bg-red-600 hover:bg-red-700 text-white rounded-xl` OR `border border-red-200 text-red-600 hover:bg-red-50`.
* **Disabled State:** `disabled:opacity-50 disabled:cursor-not-allowed` with readable contrast.

### 3.2 Cards
* **Background:** `#FFFFFF`
* **Border:** `1px solid #E7E1D8` (`border-sand-200`)
* **Corner Radius:** `rounded-xl` (12px) or `rounded-2xl` (16px)
* **Shadow:** `shadow-xs` or `shadow-sm` (subtle elevation, no heavy blur)
* **Padding:** `p-4 sm:p-6 lg:p-8`

### 3.3 Form Inputs
* **Background:** `#FFFFFF` (or `bg-sand-50` for read-only / helper boxes)
* **Border:** `1px solid #E7E1D8` / `#D9D9D9` (`border-sand-300`)
* **Typography:** `text-charcoal-900 placeholder:text-charcoal-400`
* **Focus Ring:** `focus:outline-none focus:ring-2 focus:ring-bronze-500`

### 3.4 Tables
* **Container:** `bg-white border border-sand-200 rounded-xl overflow-hidden`
* **Header Row:** `bg-sand-50/60 border-b border-sand-200 text-charcoal-500 text-[11px] font-semibold uppercase tracking-wider`
* **Body Rows:** `divide-y divide-sand-100 hover:bg-sand-50/50 transition-colors`

### 3.5 Modals & Dialogs
* **Overlay:** `fixed inset-0 bg-charcoal-900/50 backdrop-blur-xs z-50`
* **Card Container:** `bg-white border border-sand-200 rounded-2xl p-6 shadow-xl`
* **Actions:** Right-aligned Cancel (secondary/ghost) and Confirm (primary) buttons.

### 3.6 Mobile Navigation & Safe Areas
* **Fixed Bottom Bar:** `md:hidden fixed bottom-0 inset-x-0 z-40 bg-white border-t border-sand-200`
* **Main Canvas Safe Area:** `<main className="flex-1 pb-20 md:pb-0">` ensures zero occlusion of bottom controls or pagination on 360px–430px screens.

### 3.7 Platform Operations & Infrastructure Controls
* **Operations Cards:** `bg-white border border-sand-200 rounded-xl p-5 shadow-sm space-y-4`. Clean, restrained editorial composition avoiding dark DevOps or generic cloud console aesthetics.
* **Platform State Pill:**
  * `NORMAL`: `bg-emerald-50 text-emerald-800 border border-emerald-300`
  * `READ_ONLY`: `bg-amber-50 text-amber-800 border border-amber-300`
* **Provider Health Badges:**
  * `AVAILABLE`: `bg-emerald-50 text-emerald-700 border border-emerald-200`
  * `DEGRADED`: `bg-amber-50 text-amber-700 border border-amber-200`
  * `DISABLED`: `bg-sand-100 text-charcoal-600 border border-sand-300`
  * `NOT_CONFIGURED`: `bg-sand-50 text-charcoal-500 border border-sand-200`
  * `UNAVAILABLE`: `bg-red-50 text-red-700 border border-red-200`

### 3.8 High-Impact Administrative Confirmations
* **Visual Treatment:** White dialog (`bg-white border border-sand-200 rounded-xl max-w-md shadow-xl`), `backdrop-blur-xs` scrim.
* **Requirements:**
  * Explicit action explanation detailing caller impact (e.g., session revocation, mutation restrictions).
  * Mandatory audit reason input with client-side character validation (min 10 characters for MFA reset and state changes).
  * Distinct action button (`bg-amber-700 hover:bg-amber-800 text-white` for privileged step-up or restricted modes).
  * Clear loading state (`disabled:opacity-50`) preventing duplicate in-flight mutations.
